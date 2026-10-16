package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 店铺后台工作台。
 */
@Schema(description = "店铺后台工作台")
public record ShopDashboardVO(

        @Schema(description = "今日订单数")
        long todayOrderCount,

        @Schema(description = "今日支付金额（分）")
        long todayPaidAmount,

        @Schema(description = "待发货数")
        long pendingShipmentCount,

        @Schema(description = "待处理售后数")
        long pendingRefundCount,

        @Schema(description = "在售商品数")
        long onSaleProductCount
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
