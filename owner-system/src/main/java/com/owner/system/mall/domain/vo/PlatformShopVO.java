package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 平台视角下的店铺概览行。联系人手机号按 PRD §14 打星。
 */
@Schema(description = "平台视角下的店铺概览行")
public record PlatformShopVO(

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "店名")
        String name,

        @Schema(description = "联系人")
        String contactName,

        @Schema(description = "联系电话（中间四位打星）")
        String contactMobileMasked,

        @Schema(description = "状态：0 营业，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "在售商品数")
        long onSaleProductCount,

        @Schema(description = "待发货数")
        long pendingShipmentCount,

        @Schema(description = "今日支付金额（分）")
        long todayPaidAmount
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
