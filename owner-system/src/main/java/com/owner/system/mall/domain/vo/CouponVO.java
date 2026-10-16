package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券（券本身，领用情况看 {@link CouponUserVO}）。
 */
@Schema(description = "优惠券")
public record CouponVO(

        @Schema(description = "券ID")
        Long id,

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "券名称")
        String name,

        @Schema(description = "门槛金额（分）")
        Long thresholdAmount,

        @Schema(description = "减免金额（分）")
        Long reduceAmount,

        @Schema(description = "发放数量")
        Integer total,

        @Schema(description = "已领取数量")
        Integer received,

        @Schema(description = "每人限领")
        Integer perUserLimit,

        @Schema(description = "开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime startTime,

        @Schema(description = "结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime endTime,

        @Schema(description = "状态：0 启用，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "是否已过期")
        boolean expired,

        @Schema(description = "是否还能领")
        boolean receivable,

        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
