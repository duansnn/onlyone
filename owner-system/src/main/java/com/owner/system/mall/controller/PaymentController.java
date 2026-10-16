package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.JsonUtils;
import com.owner.system.mall.domain.enums.PayChannel;
import com.owner.system.mall.domain.vo.PaymentCreateVO;
import com.owner.system.mall.service.PaymentService;
import com.owner.system.mall.service.OrderService;
import com.owner.system.mall.support.PaymentGateway;
import com.owner.system.mall.support.ShopGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付接口：发起支付、平台异步通知、本地模拟回调。
 *
 * <p>未配置商户号时 {@code payable=false}，订单停在待付款。不提供「我已付款」按钮。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Tag(name = "支付", description = "发起支付、支付异步通知")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final ShopGuard shopGuard;
    private final OrderService orderService;

    @Operation(summary = "发起支付", description = "金额由服务端按订单应付生成，不接受前端传入")
    @PostMapping("/{orderId}")
    public R<PaymentCreateVO> create(
            @PathVariable Long orderId,
            @Parameter(description = "WECHAT / ALIPAY") @RequestParam String channel) {
        return R.ok(paymentService.create(orderId, channel));
    }

    @Operation(summary = "支付异步通知", description = "由微信 / 支付宝回调，无需登录")
    @PostMapping("/notify/{channel}")
    public String notify(@PathVariable String channel, @RequestBody String body) {
        PayChannel payChannel = PayChannel.of(channel);
        if (payChannel == null) {
            return "fail";
        }
        boolean ok = paymentService.handleNotify(payChannel, body);
        return ok ? "success" : "fail";
    }

    /**
     * 本地联调用的模拟回调：把一次「平台通知」交给同一个入账流程。
     * <p>只在 {@code owner.mall.pay.mock-enabled=true} 时可用；需要登录，且只能回自己的订单。</p>
     */
    @Operation(summary = "模拟支付回调（仅本地联调）", description = "需 owner.mall.pay.mock-enabled=true")
    @PostMapping("/mock/callback")
    public R<Map<String, Object>> mockCallback(@RequestBody Map<String, Object> body) {
        PayChannel channel = PayChannel.of(asString(body.get("channel")));
        if (channel == null) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "支付方式只能是 WECHAT 或 ALIPAY");
        }
        if (!paymentGateway.isEnabled(channel)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR,
                    "模拟支付未开启，请设置 owner.mall.pay.mock-enabled=true");
        }
        Long orderId = asLong(body.get("orderId"));
        if (orderId == null) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "缺少 orderId");
        }
        // 模拟：先按订单发起一次支付拿到商户单号，再走真实入账流程
        PaymentCreateVO created = paymentService.create(orderId, channel.name());
        Map<String, Object> notify = new LinkedHashMap<>();
        notify.put("outTradeNo", created.outTradeNo());
        notify.put("platformTradeNo", "MOCK" + System.currentTimeMillis());
        notify.put("amount", created.amount());

        boolean ok = paymentService.handleNotify(channel, JsonUtils.toJson(notify));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accepted", ok);
        data.put("orderId", orderId);
        data.put("orderNo", created.orderNo());
        data.put("amount", created.amount());
        data.put("tip", ok ? "已按支付成功入账" : "入账被拒绝，请看服务端日志");
        return R.ok(data);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
