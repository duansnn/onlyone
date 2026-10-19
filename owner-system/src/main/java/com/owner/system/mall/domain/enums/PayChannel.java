package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 收款渠道。
 *
 * <p><b>本项目不接入在线支付</b>，收款一律走 {@link #OFFLINE}（商家线下收钱后在后台确认）。
 * {@code WECHAT} / {@code ALIPAY} 仅作为历史数据兼容保留：老库里可能存过这两个值，
 * 读取时不能报错，但系统不会再产生新的在线支付记录。</p>
 */
@Getter
public enum PayChannel {

    /** 历史值，未接入：库里若有该值仅用于展示，不会再有新记录 */
    WECHAT("微信支付"),
    /** 历史值，未接入：库里若有该值仅用于展示，不会再有新记录 */
    ALIPAY("支付宝"),
    /** 线下收款：商家在后台确认收到钱（现金 / 转账 / 到付），系统不对接任何支付平台 */
    OFFLINE("线下收款");

    private final String label;

    PayChannel(String label) {
        this.label = label;
    }

    public static PayChannel of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
