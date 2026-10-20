package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.ProductSaveDTO;
import com.owner.system.mall.domain.dto.SkuSaveDTO;
import com.owner.system.mall.domain.entity.Category;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.OrderItem;
import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductImage;
import com.owner.system.mall.domain.entity.ProductSku;
import com.owner.system.mall.domain.enums.OrderStatus;
import com.owner.system.mall.domain.enums.ProductStatus;
import com.owner.system.mall.domain.vo.ProductAdminDetailVO;
import com.owner.system.mall.domain.vo.ProductAdminVO;
import com.owner.system.mall.mapper.CategoryMapper;
import com.owner.system.mall.mapper.OrderInfoMapper;
import com.owner.system.mall.mapper.OrderItemMapper;
import com.owner.system.mall.mapper.ProductImageMapper;
import com.owner.system.mall.mapper.ProductMapper;
import com.owner.system.mall.mapper.ProductSkuMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.ProductAssembler;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品服务：SPU + 主图 + SKU 一次保存。
 *
 * <p>上架要求：至少一张主图，至少一个启用且库存大于 0 的 SKU。
 * 不满足时保存为草稿，不能直接在售（PRD §5.2）。</p>
 * <p>删除用停用，不物理删除有过订单的商品与 SKU。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int SKU_ENABLED = 0;
    private static final int SKU_DISABLED = 1;

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductImageMapper imageMapper;
    private final CategoryMapper categoryMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final StockService stockService;
    private final ShopGuard shopGuard;
    private final ProductAssembler assembler;

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /** 后台商品分页 */
    public PageResult<ProductAdminVO> adminPage(Long shopId, String keyword, String status,
                                                Long categoryId, PageQuery query) {
        shopGuard.requireMember(shopId);
        ProductStatus statusFilter = ProductStatus.of(status);
        Page<Product> page = productMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getShopId, shopId)
                        .eq(statusFilter != null, Product::getStatus,
                                statusFilter == null ? null : statusFilter.name())
                        .eq(categoryId != null, Product::getCategoryId, categoryId)
                        .and(StringUtils.isNotBlank(keyword), w -> w
                                .like(Product::getName, keyword)
                                .or().like(Product::getSellingPoint, keyword))
                        .orderByAsc(Product::getSort)
                        .orderByDesc(Product::getCreateTime));

        List<Long> productIds = page.getRecords().stream().map(Product::getId).toList();
        Map<Long, List<ProductSku>> skuMap = assembler.skusOf(productIds);
        Map<Long, List<String>> imageMap = assembler.imagesOf(productIds);
        Map<Long, String> categoryNames = categoryNames(page.getRecords().stream()
                .map(Product::getCategoryId).distinct().toList());

        List<ProductAdminVO> rows = new ArrayList<>(page.getRecords().size());
        for (Product product : page.getRecords()) {
            List<ProductSku> skus = skuMap.getOrDefault(product.getId(), List.of());
            List<ProductSku> enabled = ProductAssembler.enabledSkus(skus);
            long totalStock = enabled.stream()
                    .mapToLong(sku -> sku.getStock() == null ? 0 : sku.getStock()).sum();
            ProductStatus productStatus = ProductStatus.of(product.getStatus());
            rows.add(new ProductAdminVO(product.getId(), product.getCategoryId(),
                    categoryNames.get(product.getCategoryId()), product.getName(),
                    product.getStatus(), productStatus == null ? product.getStatus() : productStatus.getLabel(),
                    product.getSort(), assembler.coverOf(imageMap.get(product.getId())),
                    ProductAssembler.minPrice(skus), ProductAssembler.maxPrice(skus),
                    totalStock, ProductAssembler.totalAvailable(skus), enabled.size(),
                    product.getVersion(), product.getUpdateTime()));
        }
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 后台商品详情（编辑用） */
    public ProductAdminDetailVO adminDetail(Long shopId, Long productId) {
        shopGuard.requireMember(shopId);
        Product product = requireProduct(shopId, productId);
        List<String> images = assembler.imagesOf(List.of(productId))
                .getOrDefault(productId, List.of());
        List<ProductSku> skus = assembler.skusOf(List.of(productId))
                .getOrDefault(productId, List.of());
        ProductStatus status = ProductStatus.of(product.getStatus());
        return new ProductAdminDetailVO(product.getId(), product.getCategoryId(), product.getName(),
                product.getSellingPoint(), product.getDetail(), product.getStatus(),
                status == null ? product.getStatus() : status.getLabel(), product.getSort(),
                images, assembler.toSkuVoList(skus), product.getVersion(),
                product.getCreateTime(), product.getUpdateTime());
    }

    /** 取商品（校验归属） */
    public Product requireProduct(Long shopId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !shopId.equals(product.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "商品不存在：" + productId);
        }
        return product;
    }

    // ------------------------------------------------------------------
    // 保存
    // ------------------------------------------------------------------

    /** 新建商品 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long shopId, ProductSaveDTO dto) {
        shopGuard.requireMember(shopId);
        Long categoryId = requireLeafCategory(shopId, dto.categoryId());

        Product product = new Product();
        product.setShopId(shopId);
        product.setCategoryId(categoryId);
        product.setName(dto.name().trim());
        product.setSellingPoint(StringUtils.trimToNull(dto.sellingPoint()));
        product.setDetail(dto.detail());
        product.setSort(dto.sort() == null ? 0 : dto.sort());
        product.setStatus(ProductStatus.DRAFT.name());
        product.setVersion(0);
        productMapper.insert(product);

        replaceImages(product.getId(), dto.images());
        syncSkus(shopId, product.getId(), dto.skus());
        applyStatus(productMapper.selectById(product.getId()), dto.status());

        log.info("[Product] 新建商品 shopId={}, productId={}, name={}", shopId, product.getId(), product.getName());
        return product.getId();
    }

    /** 修改商品 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long shopId, Long productId, ProductSaveDTO dto) {
        shopGuard.requireMember(shopId);
        Product product = requireProduct(shopId, productId);
        Long categoryId = requireLeafCategory(shopId, dto.categoryId());

        product.setCategoryId(categoryId);
        product.setName(dto.name().trim());
        product.setSellingPoint(StringUtils.trimToNull(dto.sellingPoint()));
        product.setDetail(dto.detail());
        if (dto.sort() != null) {
            product.setSort(dto.sort());
        }
        if (dto.version() != null) {
            product.setVersion(dto.version());
        }
        if (productMapper.updateById(product) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }

        replaceImages(productId, dto.images());
        syncSkus(shopId, productId, dto.skus());
        applyStatus(productMapper.selectById(productId), dto.status());

        log.info("[Product] 修改商品 shopId={}, productId={}", shopId, productId);
    }

    /** 上架 / 下架 */
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long shopId, Long productId, String status) {
        shopGuard.requireMember(shopId);
        Product product = requireProduct(shopId, productId);
        ProductStatus target = ProductStatus.of(status);
        if (target == null || target == ProductStatus.DRAFT) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "状态只能是 ON_SALE 或 OFF_SHELF");
        }
        if (target == ProductStatus.ON_SALE && !meetsOnSaleRequirement(productId)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    "上架需要至少一张主图，以及至少一个启用且可售的规格");
        }
        if (target.name().equals(product.getStatus())) {
            return;
        }
        product.setStatus(target.name());
        if (productMapper.updateById(product) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Product] 商品 {} 状态改为 {}", productId, target.getLabel());
    }

    /**
     * 删除商品：逻辑删除（{@code del_flag = 1}），库里留痕，误删可由 DBA 恢复。
     *
     * <p>有未完结订单（待确认 / 待发货 / 待收货 / 退款中）的商品不允许删：
     * 订单详情要靠 {@code order_item} 里的快照展示，但售后与库存回补还要认商品与规格，
     * 先下架或等订单走完再删。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long shopId, Long productId) {
        shopGuard.requireMember(shopId);
        Product product = requireProduct(shopId, productId);

        List<Long> orderIds = orderItemMapper
                .selectList(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getProductId, productId))
                .stream().map(OrderItem::getOrderId).distinct().toList();
        if (!orderIds.isEmpty()) {
            long ongoing = orderInfoMapper.selectCount(Wrappers.<OrderInfo>lambdaQuery()
                    .in(OrderInfo::getId, orderIds)
                    .in(OrderInfo::getStatus,
                            OrderStatus.PENDING_PAYMENT.name(),
                            OrderStatus.PENDING_SHIPMENT.name(),
                            OrderStatus.PENDING_RECEIPT.name(),
                            OrderStatus.REFUNDING.name()));
            if (ongoing > 0) {
                throw new ServiceException(ResultCode.BUSINESS_ERROR,
                        "还有 " + ongoing + " 笔未完结的订单包含该商品，处理完再删（也可以先下架）");
            }
        }

        List<ProductSku> skus = skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getProductId, productId));
        for (ProductSku sku : skus) {
            skuMapper.deleteById(sku.getId());
        }
        imageMapper.delete(Wrappers.<ProductImage>lambdaQuery()
                .eq(ProductImage::getProductId, productId));
        productMapper.deleteById(product.getId());

        log.info("[Product] 删除商品 shopId={}, productId={}, name={}, 规格 {} 个",
                shopId, productId, product.getName(), skus.size());
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /** 主图：整体替换，sort 从 0 递增（0 为封面） */
    private void replaceImages(Long productId, List<String> images) {
        imageMapper.delete(Wrappers.<ProductImage>lambdaQuery()
                .eq(ProductImage::getProductId, productId));
        if (images == null || images.isEmpty()) {
            return;
        }
        int sort = 0;
        for (String url : images) {
            if (StringUtils.isBlank(url)) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(url.trim());
            image.setSort(sort++);
            imageMapper.insert(image);
        }
    }

    /** 规格：有 id 的更新，无 id 的新建，本次没出现的停用 */
    private void syncSkus(Long shopId, Long productId, List<SkuSaveDTO> dtos) {
        Long operatorId = MallCurrentUser.requireUserId();
        List<ProductSku> existing = skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getProductId, productId));
        Map<Long, ProductSku> existingById = new HashMap<>(existing.size() * 2);
        for (ProductSku sku : existing) {
            existingById.put(sku.getId(), sku);
        }

        Set<Long> kept = new HashSet<>();
        for (SkuSaveDTO dto : dtos) {
            if (dto.price() == null || dto.price() <= 0) {
                throw new ServiceException(ResultCode.BAD_REQUEST, "售价必须大于 0");
            }
            int stock = dto.stock() == null ? 0 : dto.stock();
            int skuStatus = "1".equals(dto.status()) ? SKU_DISABLED : SKU_ENABLED;
            String skuCode = StringUtils.trimToNull(dto.skuCode());
            if (skuCode != null) {
                assertSkuCodeUnique(shopId, skuCode, dto.id());
            }

            if (dto.id() != null) {
                ProductSku sku = existingById.get(dto.id());
                if (sku == null) {
                    throw new ServiceException(ResultCode.DATA_NOT_FOUND, "规格不属于该商品：" + dto.id());
                }
                int locked = sku.getLockedStock() == null ? 0 : sku.getLockedStock();
                if (stock < locked) {
                    throw new ServiceException(ResultCode.BUSINESS_ERROR,
                            "库存不能小于未支付订单锁定量 " + locked);
                }
                int before = sku.getStock() == null ? 0 : sku.getStock();
                sku.setSpecName(dto.specName().trim());
                sku.setPrice(dto.price());
                sku.setListPrice(dto.listPrice());
                sku.setStock(stock);
                sku.setSkuCode(skuCode);
                sku.setWeightGram(dto.weightGram());
                sku.setStatus(skuStatus);
                if (skuMapper.updateById(sku) == 0) {
                    throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
                }
                if (stock != before) {
                    stockService.recordManual(shopId, sku.getId(), stock - before, stock, locked,
                            operatorId, "编辑商品时调整库存");
                }
                kept.add(sku.getId());
            } else {
                ProductSku sku = new ProductSku();
                sku.setProductId(productId);
                sku.setShopId(shopId);
                sku.setSpecName(dto.specName().trim());
                sku.setPrice(dto.price());
                sku.setListPrice(dto.listPrice());
                sku.setStock(stock);
                sku.setLockedStock(0);
                sku.setSkuCode(skuCode);
                sku.setWeightGram(dto.weightGram());
                sku.setStatus(skuStatus);
                sku.setVersion(0);
                skuMapper.insert(sku);
                stockService.recordInit(shopId, sku.getId(), stock, operatorId);
                kept.add(sku.getId());
            }
        }

        for (ProductSku sku : existing) {
            if (!kept.contains(sku.getId()) && (sku.getStatus() == null || sku.getStatus() == SKU_ENABLED)) {
                sku.setStatus(SKU_DISABLED);
                skuMapper.updateById(sku);
                log.info("[Product] 商品 {} 的规格 {} 未在本次提交中出现，已停用", productId, sku.getId());
            }
        }
    }

    /**
     * 决定最终上架状态：请求在售但不满足要求时降级为草稿。
     */
    private void applyStatus(Product product, String requestedStatus) {
        if (product == null) {
            return;
        }
        ProductStatus requested = ProductStatus.of(requestedStatus);
        if (requested == null) {
            requested = ProductStatus.DRAFT;
        }
        ProductStatus finalStatus = requested;
        if (requested == ProductStatus.ON_SALE && !meetsOnSaleRequirement(product.getId())) {
            finalStatus = ProductStatus.DRAFT;
            log.info("[Product] 商品 {} 不满足上架要求（需至少一张主图 + 一个可售规格），已保存为草稿",
                    product.getId());
        }
        if (finalStatus.name().equals(product.getStatus())) {
            return;
        }
        product.setStatus(finalStatus.name());
        productMapper.updateById(product);
    }

    /** 至少一张主图 + 至少一个启用且可售的规格 */
    private boolean meetsOnSaleRequirement(Long productId) {
        long imageCount = imageMapper.selectCount(Wrappers.<ProductImage>lambdaQuery()
                .eq(ProductImage::getProductId, productId));
        if (imageCount == 0) {
            return false;
        }
        List<ProductSku> skus = skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getProductId, productId));
        return skus.stream().anyMatch(sku ->
                (sku.getStatus() == null || sku.getStatus() == SKU_ENABLED) && sku.availableStock() > 0);
    }

    private void assertSkuCodeUnique(Long shopId, String skuCode, Long selfId) {
        ProductSku other = skuMapper.selectOne(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getShopId, shopId)
                .eq(ProductSku::getSkuCode, skuCode)
                .ne(selfId != null, ProductSku::getId, selfId)
                .last("LIMIT 1"));
        if (other != null) {
            throw new ServiceException(ResultCode.DATA_EXISTS, "货号已存在：" + skuCode);
        }
    }

    /** 商品必须选到二级类目 */
    private Long requireLeafCategory(Long shopId, Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !shopId.equals(category.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "类目不存在：" + categoryId);
        }
        if (category.getParentId() == null || category.getParentId() == 0L) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "类目必须选到二级");
        }
        return category.getId();
    }

    private Map<Long, String> categoryNames(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>(categoryIds.size() * 2);
        for (Category category : categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .in(Category::getId, categoryIds))) {
            result.put(category.getId(), category.getName());
        }
        return result;
    }
}
