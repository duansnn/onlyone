package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存页的 SKU 行。
 */
@Schema(description = "库存页 SKU 行")
public record SkuStockVO(

        @Schema(description = "规格ID")
        Long skuId,

        @Schema(description = "商品ID")
        Long productId,

        @Schema(description = "商品名")
        String productName,

        @Schema(description = "规格名")
        String specName,

        @Schema(description = "货号")
        String skuCode,

        @Schema(description = "库存")
        Integer stock,

        @Schema(description = "锁定量")
        Integer lockedStock,

        @Schema(description = "可售库存")
        Integer availableStock,

        @Schema(description = "状态：0 启用，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
