package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 库存变动原因（写 stock_log 时使用）。
 */
@Getter
public enum StockReason {

    /** 初始入库 */
    INIT("初始入库"),
    /** 手动调整 */
    MANUAL("手动调整"),
    /** 下单锁定 */
    LOCK("下单锁定"),
    /** 支付扣减 */
    PAY("支付扣减"),
    /** 取消释放 */
    CANCEL("取消释放"),
    /** 售后退回 */
    RETURN("售后退回");

    private final String label;

    StockReason(String label) {
        this.label = label;
    }

    /** 解析，未知返回 null */
    public static StockReason of(String code) {
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
