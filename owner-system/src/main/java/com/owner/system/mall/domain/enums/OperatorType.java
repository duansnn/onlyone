package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 订单状态记录的操作方。
 */
@Getter
public enum OperatorType {

    CUSTOMER("顾客"),
    SHOP("店铺"),
    PLATFORM("平台"),
    SYSTEM("系统");

    private final String label;

    OperatorType(String label) {
        this.label = label;
    }
}
