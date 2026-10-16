package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 确认订单页计价结果。
 * <p>失效行会被拦下；下单时若价格变化，也会通过这里把新价格回显给顾客。</p>
 */
@Schema(description = "确认订单页计价结果")
public record OrderPreviewVO(

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "店名")
        String shopName,

        @Schema(description = "可结算的商品行")
        List<OrderItemVO> items,

        @Schema(description = "被拦下的行（已下架 / 规格停用 / 库存不足）")
        List<CartItemVO> invalidItems,

        @Schema(description = "商品总额（分）")
        long goodsAmount,

        @Schema(description = "运费（分），本期固定 0")
        long freightAmount,

        @Schema(description = "优惠额（分）")
        long discountAmount,

        @Schema(description = "应付（分）")
        long payAmount,

        @Schema(description = "当前选用的优惠券领用记录ID")
        Long couponUserId,

        @Schema(description = "当前优惠券名称")
        String couponName,

        @Schema(description = "可用优惠券")
        List<CouponUserVO> usableCoupons,

        @Schema(description = "提示信息（如「部分商品已下架，已为你移除」）")
        String message
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
