package com.owner.system.mall.support;

import com.owner.common.core.utils.JsonUtils;
import com.owner.system.mall.domain.entity.Payment;
import com.owner.system.mall.domain.enums.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟支付通道，仅在 {@code owner.mall.pay.mock-enabled=true} 时生效。
 *
 * <p><b>只用于本地联调</b>：它让未接商户号时也能把「待发货 → 发货 → 收货 → 售后 → 评价」
 * 这条链路走完。它不改变任何业务规则——模拟付款同样要经过
 * {@code PaymentService.handleNotify} 的金额校验与幂等入账，只是把「平台回调」这一步
 * 换成了本地生成的通知。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "owner.mall.pay", name = "mock-enabled", havingValue = "true")
public class MockPaymentGateway implements PaymentGateway {

    public MockPaymentGateway() {
        log.warn("[Pay] 已启用【模拟支付】通道（owner.mall.pay.mock-enabled=true），"
                + "仅用于本地联调，请勿在生产环境开启");
    }

    @Override
    public boolean isEnabled(PayChannel channel) {
        return true;
    }

    @Override
    public String prepay(Payment payment, String subject) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mock", true);
        data.put("channel", payment.getChannel());
        data.put("outTradeNo", payment.getOutTradeNo());
        data.put("amount", payment.getAmount());
        data.put("subject", subject);
        data.put("tip", "模拟支付：调用 POST /api/pay/mock/callback 完成付款");
        return JsonUtils.toJson(data);
    }

    @Override
    public PaymentNotify parseNotify(PayChannel channel, String body) {
        Map<String, Object> data = JsonUtils.parseMap(body);
        if (data.isEmpty()) {
            log.warn("[Pay] 模拟通知内容无法解析：{}", body);
            return null;
        }
        String outTradeNo = asString(data.get("outTradeNo"));
        if (outTradeNo == null) {
            log.warn("[Pay] 模拟通知缺少 outTradeNo：{}", body);
            return null;
        }
        return new PaymentNotify(outTradeNo, asString(data.get("platformTradeNo")),
                asLong(data.get("amount")), true, body);
    }

    @Override
    public boolean refund(Payment payment, long amount, String reason) {
        log.warn("[Pay] 【模拟退款】outTradeNo={}, amount={}分, 原因={}",
                payment.getOutTradeNo(), amount, reason);
        return true;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
