package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 支付单状态。
 */
@Getter
public enum PayStatus {

    PENDING("待支付"),
    SUCCESS("成功"),
    FAILED("失败");

    private final String label;

    PayStatus(String label) {
        this.label = label;
    }
}
