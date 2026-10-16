package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一张领取到手的优惠券（含券本身的规则）。
 */
@Schema(description = "我的优惠券")
public record CouponUserVO(

        @Schema(description = "领用记录ID（下单时传这个）")
        Long id,

        @Schema(description = "券ID")
        Long couponId,

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "券名称")
        String name,

        @Schema(description = "门槛金额（分），商品总额达到才能用")
        Long thresholdAmount,

        @Schema(description = "减免金额（分）")
        Long reduceAmount,

        @Schema(description = "每人限领")
        Integer perUserLimit,

        @Schema(description = "开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime startTime,

        @Schema(description = "结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime endTime,

        @Schema(description = "状态：UNUSED / LOCKED / USED / RETURNED / EXPIRED")
        String status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "占用 / 使用的订单")
        Long orderId,

        @Schema(description = "是否已过期（按结束时间判断）")
        boolean expired,

        @Schema(description = "领取时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime receiveTime,

        @Schema(description = "使用时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime useTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
