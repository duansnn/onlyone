package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 支付渠道。
 */
@Getter
public enum PayChannel {

    WECHAT("微信支付"),
    ALIPAY("支付宝");

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
