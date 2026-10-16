package com.owner.system.mall.support;

import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.mall.domain.entity.Payment;
import com.owner.system.mall.domain.enums.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 未开通的支付通道（默认实现）。
 * <p>没有商户号时，支付页只标明「支付未开通」，不生成假的支付成功。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "owner.mall.pay", name = "mock-enabled",
        havingValue = "false", matchIfMissing = true)
public class DisabledPaymentGateway implements PaymentGateway {

    public DisabledPaymentGateway() {
        log.info("[Pay] 未配置商户号，支付通道为「未开通」状态；订单会停在待付款。"
                + "本地联调可设置 owner.mall.pay.mock-enabled=true 打开模拟支付。");
    }

    @Override
    public boolean isEnabled(PayChannel channel) {
        return false;
    }

    @Override
    public String prepay(Payment payment, String subject) {
        throw new ServiceException(ResultCode.BUSINESS_ERROR, "支付未开通");
    }

    @Override
    public PaymentNotify parseNotify(PayChannel channel, String body) {
        log.warn("[Pay] 收到 {} 的异步通知，但支付通道未开通，已忽略", channel);
        return null;
    }

    @Override
    public boolean refund(Payment payment, long amount, String reason) {
        log.warn("[Pay] 请求退款 {} 分（outTradeNo={}），但支付通道未开通，退款未发起",
                amount, payment == null ? null : payment.getOutTradeNo());
        return false;
    }
}
