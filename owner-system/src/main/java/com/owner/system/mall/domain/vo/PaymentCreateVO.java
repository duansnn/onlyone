package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发起支付的结果。
 * <p>{@code payable} 为 false 时表示支付通道未开通，订单仍停在待付款。</p>
 */
@Schema(description = "发起支付结果")
public record PaymentCreateVO(

        @Schema(description = "订单ID")
        Long orderId,

        @Schema(description = "订单号")
        String orderNo,

        @Schema(description = "应付金额（分）")
        long amount,

        @Schema(description = "支付方式")
        String channel,

        @Schema(description = "支付方式中文名")
        String channelLabel,

        @Schema(description = "是否可以直接支付")
        boolean payable,

        @Schema(description = "商户单号")
        String outTradeNo,

        @Schema(description = "唤起收银台的参数（JSON 字符串）")
        String prepayPayload,

        @Schema(description = "提示信息")
        String message
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
