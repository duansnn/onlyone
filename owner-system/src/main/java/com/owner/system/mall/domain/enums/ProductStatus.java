package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 商品上架状态。
 */
@Getter
public enum ProductStatus {

    /** 草稿：不满足上架要求时保存为该状态 */
    DRAFT("草稿"),
    /** 在售 */
    ON_SALE("在售"),
    /** 已下架 */
    OFF_SHELF("已下架");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public static ProductStatus of(String code) {
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
