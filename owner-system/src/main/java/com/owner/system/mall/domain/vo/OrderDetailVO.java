package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 订单详情。
 */
@Schema(description = "订单详情")
public record OrderDetailVO(

        @Schema(description = "订单摘要（含可执行动作）")
        OrderVO summary,

        @Schema(description = "收件人（完整）")
        String receiverName,

        @Schema(description = "收件手机（完整，发货要用）")
        String receiverMobile,

        @Schema(description = "收货地址")
        String receiverAddress,

        @Schema(description = "买家留言")
        String buyerRemark,

        @Schema(description = "支付渠道")
        String payChannel,

        @Schema(description = "支付平台单号")
        String payNo,

        @Schema(description = "实付金额（分）")
        Long paidAmount,

        @Schema(description = "商品行")
        List<OrderItemVO> items,

        @Schema(description = "状态记录")
        List<OrderLogVO> logs,

        @Schema(description = "当前售后单，没有则为 null")
        RefundVO currentRefund
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
