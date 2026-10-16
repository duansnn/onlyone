package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订单商品行（下单快照）。
 */
@Schema(description = "订单商品行")
public record OrderItemVO(

        @Schema(description = "行ID")
        Long id,

        @Schema(description = "订单ID")
        Long orderId,

        @Schema(description = "商品ID")
        Long productId,

        @Schema(description = "规格ID")
        Long skuId,

        @Schema(description = "商品名（快照）")
        String productName,

        @Schema(description = "规格名（快照）")
        String specName,

        @Schema(description = "封面图（快照）")
        String imageUrl,

        @Schema(description = "单价（快照，分）")
        Long price,

        @Schema(description = "数量")
        Integer quantity,

        @Schema(description = "小计（分）")
        Long amount
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
