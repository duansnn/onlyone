package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 店铺成员角色。
 */
@Getter
public enum ShopRole {

    /** 店主：可改店资料、商品、订单、店员、优惠券、售后 */
    OWNER("店主"),
    /** 店员：可改库存、上下架、发货、处理本店售后 */
    STAFF("店员");

    private final String label;

    ShopRole(String label) {
        this.label = label;
    }

    public static ShopRole of(String code) {
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
