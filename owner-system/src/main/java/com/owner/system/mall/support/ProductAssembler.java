package com.owner.system.mall.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductImage;
import com.owner.system.mall.domain.entity.ProductSku;
import com.owner.system.mall.domain.vo.ProductCardVO;
import com.owner.system.mall.domain.vo.SkuVO;
import com.owner.system.mall.mapper.ProductImageMapper;
import com.owner.system.mall.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品出参组装：批量取主图与规格，避免逐条查询（N+1）。
 */
@Component
@RequiredArgsConstructor
public class ProductAssembler {

    /** 规格状态：启用 */
    public static final int SKU_ENABLED = 0;

    private final ProductImageMapper productImageMapper;
    private final ProductSkuMapper productSkuMapper;

    /** 批量取主图，封面（sort 最小）在第一位 */
    public Map<Long, List<String>> imagesOf(Collection<Long> productIds) {
        Map<Long, List<String>> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        List<ProductImage> images = productImageMapper.selectList(Wrappers.<ProductImage>lambdaQuery()
                .in(ProductImage::getProductId, productIds)
                .orderByAsc(ProductImage::getSort)
                .orderByAsc(ProductImage::getId));
        for (ProductImage image : images) {
            result.computeIfAbsent(image.getProductId(), k -> new ArrayList<>()).add(image.getUrl());
        }
        return result;
    }

    /** 批量取规格，按 id 升序 */
    public Map<Long, List<ProductSku>> skusOf(Collection<Long> productIds) {
        Map<Long, List<ProductSku>> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        List<ProductSku> skus = productSkuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .in(ProductSku::getProductId, productIds)
                .orderByAsc(ProductSku::getId));
        for (ProductSku sku : skus) {
            result.computeIfAbsent(sku.getProductId(), k -> new ArrayList<>()).add(sku);
        }
        return result;
    }

    public SkuVO toSkuVo(ProductSku sku) {
        return new SkuVO(sku.getId(), sku.getSpecName(), sku.getPrice(), sku.getListPrice(),
                sku.getStock(), sku.getLockedStock(), sku.availableStock(), sku.getSkuCode(),
                sku.getWeightGram(), sku.getStatus(), skuStatusLabel(sku.getStatus()));
    }

    /**
     * 顾客端规格：<b>不下发划线价</b>。
     * <p>商品的成本价、划线价是店主自己看的经营数据，顾客只该看到最终售价，
     * 所以这里把 listPrice 置空，配合 {@code SkuVO} 上的 {@code NON_NULL} 直接不输出该字段。</p>
     */
    public SkuVO toStoreSkuVo(ProductSku sku) {
        return new SkuVO(sku.getId(), sku.getSpecName(), sku.getPrice(), null,
                sku.getStock(), sku.getLockedStock(), sku.availableStock(), sku.getSkuCode(),
                sku.getWeightGram(), sku.getStatus(), skuStatusLabel(sku.getStatus()));
    }

    public List<SkuVO> toSkuVoList(Collection<ProductSku> skus) {
        List<SkuVO> rows = new ArrayList<>();
        if (skus != null) {
            for (ProductSku sku : skus) {
                rows.add(toSkuVo(sku));
            }
        }
        return rows;
    }

    /** 顾客端规格列表，见 {@link #toStoreSkuVo} */
    public List<SkuVO> toStoreSkuVoList(Collection<ProductSku> skus) {
        List<SkuVO> rows = new ArrayList<>();
        if (skus != null) {
            for (ProductSku sku : skus) {
                rows.add(toStoreSkuVo(sku));
            }
        }
        return rows;
    }

    /** 商品卡片 */
    public ProductCardVO toCard(Product product, List<String> images, List<ProductSku> skus) {
        return new ProductCardVO(product.getId(), product.getName(), product.getSellingPoint(),
                coverOf(images), minPrice(skus), maxPrice(skus), totalAvailable(skus));
    }

    /** 封面：第一张主图 */
    public String coverOf(List<String> images) {
        return images == null || images.isEmpty() ? null : images.get(0);
    }

    /** 可售规格（启用） */
    public static List<ProductSku> enabledSkus(List<ProductSku> skus) {
        if (skus == null) {
            return List.of();
        }
        return skus.stream()
                .filter(sku -> sku.getStatus() == null || sku.getStatus() == SKU_ENABLED)
                .toList();
    }

    /** 是否存在可售规格 */
    public static boolean hasAvailableSku(List<ProductSku> skus) {
        return totalAvailable(skus) > 0;
    }

    /** 可售合计（只算启用规格） */
    public static long totalAvailable(List<ProductSku> skus) {
        long total = 0;
        for (ProductSku sku : enabledSkus(skus)) {
            total += sku.availableStock();
        }
        return total;
    }

    public static long minPrice(List<ProductSku> skus) {
        return enabledSkus(skus).stream()
                .map(ProductSku::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(0L);
    }

    public static long maxPrice(List<ProductSku> skus) {
        return enabledSkus(skus).stream()
                .map(ProductSku::getPrice)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0L);
    }

    public static String skuStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == SKU_ENABLED ? "启用" : "停用";
    }
}
