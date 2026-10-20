package com.owner.system.mall.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 售后状态。
 */
@Getter
public enum RefundStatus {

    /** 待处理 */
    PENDING("待处理"),
    /** 已同意（未发货仅退款：等待退款入账；退货退款：等待顾客填单号） */
    AGREED("已同意"),
    /** 待退货：店里已同意退货，等顾客填退货运单号 */
    WAITING_RETURN("待退货"),
    /** 已收货：店里确认收到退货，等待发起退款 */
    RECEIVED("已收货"),
    /** 已拒绝 */
    REJECTED("已拒绝"),
    /** 退款成功 */
    SUCCESS("退款成功");

    private final String label;

    RefundStatus(String label) {
        this.label = label;
    }

    /** 是否还在处理中（会挡住发货、评价） */
    public boolean isProcessing() {
        return this == PENDING || this == AGREED || this == WAITING_RETURN || this == RECEIVED;
    }

    /** 结束状态 */
    public boolean isEnd() {
        return this == REJECTED || this == SUCCESS;
    }

    /**
     * 未完结的售后状态码（PENDING / AGREED / WAITING_RETURN / RECEIVED）。
     *
     * <p>看板「待处理售后」的计数和售后列表「待处理」筛选都取这一份，
     * 两处各写一遍就会出现「看板 0 单、列表 30 条」这种对不上的情况。</p>
     */
    public static List<String> openCodes() {
        return Arrays.stream(values())
                .filter(RefundStatus::isProcessing)
                .map(Enum::name)
                .toList();
    }

    public static RefundStatus of(String code) {
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
