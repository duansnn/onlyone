package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.mall.domain.dto.CartLineDTO;
import com.owner.system.mall.domain.entity.CartItem;
import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductSku;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.enums.ProductStatus;
import com.owner.system.mall.domain.vo.CartItemVO;
import com.owner.system.mall.domain.vo.CartShopGroupVO;
import com.owner.system.mall.domain.vo.CartVO;
import com.owner.system.mall.mapper.CartItemMapper;
import com.owner.system.mall.mapper.ProductMapper;
import com.owner.system.mall.mapper.ProductSkuMapper;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.ProductAssembler;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务。
 * <p>同一用户同一 SKU 只有一条；失效行（下架、规格停用、库存为 0）单独归到
 * {@code invalidItems}，不可勾选但可删除。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    /** 单次最多 99 件 */
    public static final int MAX_QUANTITY = 99;

    private static final int SKU_ENABLED = 0;
    private static final int SHOP_ACTIVE = 0;

    private final CartItemMapper cartItemMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final ShopGuard shopGuard;
    private final ProductAssembler assembler;

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /** 我的购物车，按店铺分组 */
    public CartVO list() {
        Long userId = MallCurrentUser.requireUserId();
        List<CartItem> items = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdateTime));
        if (items.isEmpty()) {
            return new CartVO(List.of(), 0, 0);
        }

        List<Long> skuIds = items.stream().map(CartItem::getSkuId).toList();
        Map<Long, ProductSku> skuMap = new HashMap<>(skuIds.size() * 2);
        for (ProductSku sku : skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .in(ProductSku::getId, skuIds))) {
            skuMap.put(sku.getId(), sku);
        }
        List<Long> productIds = skuMap.values().stream()
                .map(ProductSku::getProductId).distinct().toList();
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Product product : productMapper.selectList(Wrappers.<Product>lambdaQuery()
                    .in(Product::getId, productIds))) {
                productMap.put(product.getId(), product);
            }
        }
        Map<Long, List<String>> imageMap = assembler.imagesOf(productMap.keySet());

        List<Long> shopIds = items.stream().map(CartItem::getShopId).distinct().toList();
        Map<Long, Shop> shopMap = new HashMap<>();
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds))) {
            shopMap.put(shop.getId(), shop);
        }

        Map<Long, List<CartItemVO>> validByShop = new LinkedHashMap<>();
        Map<Long, List<CartItemVO>> invalidByShop = new LinkedHashMap<>();
        for (CartItem item : items) {
            ProductSku sku = skuMap.get(item.getSkuId());
            Product product = sku == null ? null : productMap.get(sku.getProductId());
            Shop shop = shopMap.get(item.getShopId());

            String invalidReason = invalidReason(shop, product, sku);
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (invalidReason == null && sku != null && quantity > sku.availableStock()) {
                invalidReason = "库存不足，仅剩 " + sku.availableStock() + " 件";
            }
            boolean valid = invalidReason == null;
            Integer available = sku == null ? 0 : sku.availableStock();
            Long price = sku == null ? null : sku.getPrice();
            CartItemVO vo = new CartItemVO(item.getId(), item.getSkuId(),
                    sku == null ? null : sku.getProductId(),
                    product == null ? null : product.getName(),
                    sku == null ? null : sku.getSpecName(),
                    sku == null || imageMap.get(sku.getProductId()) == null
                            || imageMap.get(sku.getProductId()).isEmpty()
                            ? null : imageMap.get(sku.getProductId()).get(0),
                    price, quantity,
                    price == null ? null : price * quantity,
                    available, valid, invalidReason);
            if (valid) {
                validByShop.computeIfAbsent(item.getShopId(), k -> new ArrayList<>()).add(vo);
            } else {
                invalidByShop.computeIfAbsent(item.getShopId(), k -> new ArrayList<>()).add(vo);
            }
        }

        List<CartShopGroupVO> groups = new ArrayList<>();
        for (Long shopId : unionKeys(validByShop, invalidByShop)) {
            Shop shop = shopMap.get(shopId);
            groups.add(new CartShopGroupVO(shopId, shop == null ? null : shop.getName(),
                    shop == null ? null : shop.getStatus(),
                    validByShop.getOrDefault(shopId, List.of()),
                    invalidByShop.getOrDefault(shopId, List.of())));
        }
        long validCount = validByShop.values().stream().mapToLong(List::size).sum();
        return new CartVO(groups, items.size(), validCount);
    }

    /** 购物车行数 */
    public long count() {
        Long userId = MallCurrentUser.requireUserId();
        return cartItemMapper.selectCount(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId));
    }

    // ------------------------------------------------------------------
    // 变更
    // ------------------------------------------------------------------

    /** 加购：同一 SKU 数量相加，超过库存与 99 时收敛 */
    @Transactional(rollbackFor = Exception.class)
    public int add(Long skuId, int quantity) {
        Long userId = MallCurrentUser.requireUserId();
        ProductSku sku = requirePurchasableSku(skuId);
        CartItem existing = findItem(userId, skuId);
        int base = existing == null || existing.getQuantity() == null ? 0 : existing.getQuantity();
        int target = clampQuantity(base + quantity, sku.availableStock());
        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setShopId(sku.getShopId());
            item.setSkuId(skuId);
            item.setQuantity(target);
            item.setVersion(0);
            cartItemMapper.insert(item);
        } else if (target != existing.getQuantity()) {
            existing.setQuantity(target);
            cartItemMapper.updateById(existing);
        }
        return target;
    }

    /** 改数量：0 表示删除该行；超过库存 / 99 会被收敛 */
    @Transactional(rollbackFor = Exception.class)
    public int updateQuantity(Long skuId, int quantity) {
        Long userId = MallCurrentUser.requireUserId();
        CartItem item = findItem(userId, skuId);
        if (item == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "购物车里没有该规格");
        }
        if (quantity <= 0) {
            cartItemMapper.deleteById(item.getId());
            return 0;
        }
        ProductSku sku = skuMapper.selectById(skuId);
        int available = sku == null ? 0 : sku.availableStock();
        if (available <= 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "该规格暂时缺货");
        }
        int target = clampQuantity(quantity, available);
        if (target != item.getQuantity()) {
            item.setQuantity(target);
            if (cartItemMapper.updateById(item) == 0) {
                throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
            }
        }
        return target;
    }

    /** 删除一行 */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long skuId) {
        Long userId = MallCurrentUser.requireUserId();
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, skuId));
    }

    /** 清空购物车 */
    @Transactional(rollbackFor = Exception.class)
    public void clear() {
        Long userId = MallCurrentUser.requireUserId();
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    }

    /** 合并本机购物车：同一 SKU 数量相加，超过库存时收成库存数 */
    @Transactional(rollbackFor = Exception.class)
    public void merge(CartLineDTO line) {
        add(line.skuId(), line.quantity());
    }

    /** 下单成功后把这些规格从购物车移除 */
    public void removeOwned(Long userId, Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getSkuId, skuIds));
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /** 供订单模块复用：校验规格当前是否可买（未停用、商品在售、有可售库存） */
    public ProductSku requirePurchasableSku(Long skuId) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "商品规格不存在");
        }
        shopGuard.loadActiveShop(sku.getShopId());
        if (sku.getStatus() != null && sku.getStatus() != SKU_ENABLED) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "该规格已停用");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || !ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "商品已下架");
        }
        if (sku.availableStock() <= 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "该规格暂时缺货");
        }
        return sku;
    }

    private String invalidReason(Shop shop, Product product, ProductSku sku) {
        if (shop == null || (shop.getStatus() != null && shop.getStatus() != SHOP_ACTIVE)) {
            return "店铺已停用";
        }
        if (sku == null) {
            return "商品已不存在";
        }
        if (product == null || !ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            return "商品已下架";
        }
        if (sku.getStatus() != null && sku.getStatus() != SKU_ENABLED) {
            return "规格已停用";
        }
        if (sku.availableStock() <= 0) {
            return "库存不足";
        }
        return null;
    }

    private CartItem findItem(Long userId, Long skuId) {
        return cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, skuId)
                .last("LIMIT 1"));
    }

    private int clampQuantity(int quantity, int available) {
        int upper = Math.min(MAX_QUANTITY, Math.max(available, 1));
        return Math.max(1, Math.min(quantity, upper));
    }

    private List<Long> unionKeys(Map<Long, ?> a, Map<Long, ?> b) {
        List<Long> keys = new ArrayList<>(a.keySet());
        for (Long key : b.keySet()) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys;
    }
}
