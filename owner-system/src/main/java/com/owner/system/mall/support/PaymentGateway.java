package com.owner.system.mall.support;

import com.owner.system.mall.domain.entity.Payment;
import com.owner.system.mall.domain.enums.PayChannel;

/**
 * 支付通道扩展点。
 *
 * <p>国内只接微信支付和支付宝。当前工程未配置商户号，因此默认注册的是
 * {@link DisabledPaymentGateway}：支付页显示「支付未开通」，订单停在待付款，
 * 不提供「我已付款」这类按钮，避免账对不上。</p>
 *
 * <p>接入真实通道时，实现本接口并注册为 Bean（替换默认实现），
 * 在 {@link #prepay} 里调用微信/支付宝下单接口，在 {@link #parseNotify}
 * 里做验签与字段映射即可；业务侧的入账逻辑（{@code PaymentService.handleNotify}）不用改。</p>
 */
public interface PaymentGateway {

    /** 通道是否已开通 */
    boolean isEnabled(PayChannel channel);

    /**
     * 下预付单。
     *
     * @param payment 已落库的支付单（金额取自订单应付，不接受前端传入）
     * @param subject 商品标题
     * @return 交给前端唤起收银台的参数（JSON 字符串）
     */
    String prepay(Payment payment, String subject);

    /** 解析并校验异步通知，签名不对时返回 null */
    PaymentNotify parseNotify(PayChannel channel, String body);

    /**
     * 发起退款（原路退回）。
     *
     * <p>金额由服务端从售后单读取，不接受前端传入。返回 {@code false} 表示通道未开通或平台拒收，
     * 调用方必须据此失败，不能把订单改成已退款。</p>
     *
     * @param payment 该订单已入账的支付单
     * @param amount  退款金额（分）
     * @param reason  退款原因
     * @return true 表示通道已受理
     */
    boolean refund(Payment payment, long amount, String reason);
}
