package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 顾客详情里的订单行。字段只够在抽屉里列表展示，
 * 要看完整订单（物流、日志）仍走订单模块。
 */
@Schema(description = "顾客详情-订单行")
public record CustomerOrderVO(

        @Schema(description = "订单ID")
        Long id,

        @Schema(description = "订单号")
        String orderNo,

        @Schema(description = "状态")
        String status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "应付金额（分）")
        long payAmount,

        @Schema(description = "件数")
        int itemCount,

        @Schema(description = "商品摘要，如「抽纸 等 2 件」")
        String productSummary,

        @Schema(description = "首行商品封面")
        String coverImage,

        @Schema(description = "下单时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
