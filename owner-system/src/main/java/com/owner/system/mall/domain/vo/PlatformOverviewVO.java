package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 平台工作台：店铺数、全部待发货数、今日支付金额。
 */
@Schema(description = "平台工作台")
public record PlatformOverviewVO(

        @Schema(description = "店铺总数")
        long shopCount,

        @Schema(description = "营业中店铺数")
        long activeShopCount,

        @Schema(description = "全部待发货数")
        long pendingShipmentCount,

        @Schema(description = "全部待处理售后数")
        long pendingRefundCount,

        @Schema(description = "今日订单数")
        long todayOrderCount,

        @Schema(description = "今日支付金额（分）")
        long todayPaidAmount
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
