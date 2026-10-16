package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 优惠券领用状态。
 */
@Getter
public enum CouponUserStatus {

    /** 未使用 */
    UNUSED("未使用"),
    /** 已占用：提交订单时占用，取消或超时未支付则退回 */
    LOCKED("已占用"),
    /** 已使用：支付成功后核销 */
    USED("已使用"),
    /** 已退回 */
    RETURNED("已退回"),
    /** 已过期 */
    EXPIRED("已过期");

    private final String label;

    CouponUserStatus(String label) {
        this.label = label;
    }

    /** 可以再次用于结算 */
    public boolean isAvailable() {
        return this == UNUSED || this == RETURNED;
    }

    public static CouponUserStatus of(String code) {
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
