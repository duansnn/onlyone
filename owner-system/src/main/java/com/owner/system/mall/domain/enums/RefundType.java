package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 售后类型。
 */
@Getter
public enum RefundType {

    /** 未发货：仅退款 */
    REFUND_ONLY("仅退款"),
    /** 已发货或已完成 7 天内：退货退款 */
    RETURN_REFUND("退货退款");

    private final String label;

    RefundType(String label) {
        this.label = label;
    }

    public static RefundType of(String code) {
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
