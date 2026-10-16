package com.owner.system.mall.support;

import com.owner.system.mall.domain.enums.PayChannel;

/**
 * 归一化后的支付异步通知。
 */
public record PaymentNotify(

        /** 商户单号，对应 payment.out_trade_no */
        String outTradeNo,

        /** 支付平台单号 */
        String platformTradeNo,

        /** 平台回调的金额（分），必须等于订单应付 */
        long amount,

        /** 平台是否判定支付成功 */
        boolean success,

        /** 通知原文 */
        String rawBody
) {
}
