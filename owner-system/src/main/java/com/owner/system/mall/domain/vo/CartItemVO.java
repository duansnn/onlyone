package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 购物车一行。失效行（下架、规格停用、库存为 0）的 {@code valid} 为 false。
 */
@Schema(description = "购物车行")
public record CartItemVO(

        @Schema(description = "购物车行ID")
        Long id,

        @Schema(description = "规格ID")
        Long skuId,

        @Schema(description = "商品ID")
        Long productId,

        @Schema(description = "商品名")
        String productName,

        @Schema(description = "规格名")
        String specName,

        @Schema(description = "封面图")
        String imageUrl,

        @Schema(description = "单价（分）")
        Long price,

        @Schema(description = "数量")
        Integer quantity,

        @Schema(description = "小计（分）")
        Long amount,

        @Schema(description = "当前可售库存")
        Integer availableStock,

        @Schema(description = "是否可结算")
        boolean valid,

        @Schema(description = "失效原因，可结算时为 null")
        String invalidReason
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
