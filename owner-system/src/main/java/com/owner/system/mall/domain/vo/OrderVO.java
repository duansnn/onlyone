package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单列表行。
 */
@Schema(description = "订单列表行")
public record OrderVO(

        @Schema(description = "订单ID")
        Long id,

        @Schema(description = "订单号")
        String orderNo,

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "店名")
        String shopName,

        @Schema(description = "状态")
        String status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "商品总额（分）")
        long goodsAmount,

        @Schema(description = "运费（分）")
        long freightAmount,

        @Schema(description = "优惠额（分）")
        long discountAmount,

        @Schema(description = "应付（分）")
        long payAmount,

        @Schema(description = "件数")
        int itemCount,

        @Schema(description = "商品名汇总，如「纸巾 等 2 件商品」")
        String productSummary,

        @Schema(description = "首行商品封面")
        String coverImage,

        @Schema(description = "收件手机（中间四位打星）")
        String receiverMobileMasked,

        @Schema(description = "快递公司")
        String shipCompany,

        @Schema(description = "运单号")
        String shipNo,

        @Schema(description = "取消原因")
        String cancelReason,

        @Schema(description = "下单时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime,

        @Schema(description = "支付时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime payTime,

        @Schema(description = "发货时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime shipTime,

        @Schema(description = "完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime finishTime,

        @Schema(description = "取消时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime cancelTime,

        @Schema(description = "待付款超时时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime expireTime,

        @Schema(description = "可以取消")
        boolean canCancel,

        @Schema(description = "可以确认收货")
        boolean canConfirmReceipt,

        @Schema(description = "可以评价")
        boolean canReview,

        @Schema(description = "可以申请售后")
        boolean canApplyRefund,

        @Schema(description = "可以发货（后台用）")
        boolean canShip
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
