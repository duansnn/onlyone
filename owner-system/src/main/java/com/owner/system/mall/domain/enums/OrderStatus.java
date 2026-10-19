package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 订单主状态。一笔订单同一时刻只有一个主状态。
 */
@Getter
public enum OrderStatus {

    /** 已下单，库存已锁定，等待商家确认收款（系统不接入在线支付） */
    PENDING_PAYMENT("待确认"),
    /** 已支付，等待发货 */
    PENDING_SHIPMENT("待发货"),
    /** 已填运单，等待收货 */
    PENDING_RECEIPT("待收货"),
    /** 顾客确认收货，或发货满 7 天自动确认 */
    FINISHED("已完成"),
    /** 顾客取消，或 30 分钟未支付 */
    CANCELLED("已取消"),
    /** 售后处理中 */
    REFUNDING("退款中"),
    /** 钱已退回 */
    REFUNDED("已退款");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** 结束状态：已取消 / 已退款 */
    public boolean isEnd() {
        return this == CANCELLED || this == REFUNDED;
    }

    /** 尚未付款 */
    public boolean isUnpaid() {
        return this == PENDING_PAYMENT;
    }

    /** 是否已经付过款（排除待付款与已取消） */
    public boolean isPaid() {
        return this != PENDING_PAYMENT && this != CANCELLED;
    }

    /** 顾客可以评价：已完成 */
    public boolean canReview() {
        return this == FINISHED;
    }

    /** 解析，未知返回 null */
    public static OrderStatus of(String code) {
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
