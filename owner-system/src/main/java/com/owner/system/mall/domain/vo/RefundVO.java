package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 售后单。
 */
@Schema(description = "售后单")
public record RefundVO(

        @Schema(description = "售后ID")
        Long id,

        @Schema(description = "订单ID")
        Long orderId,

        @Schema(description = "订单号")
        String orderNo,

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "店名")
        String shopName,

        @Schema(description = "顾客ID")
        Long userId,

        @Schema(description = "售后类型")
        String type,

        @Schema(description = "类型中文名")
        String typeLabel,

        @Schema(description = "原因")
        String reason,

        @Schema(description = "说明")
        String description,

        @Schema(description = "状态")
        String status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "退款金额（分）")
        Long refundAmount,

        @Schema(description = "拒绝理由")
        String refuseReason,

        @Schema(description = "退货快递公司")
        String returnCompany,

        @Schema(description = "退货运单号")
        String returnShipNo,

        @Schema(description = "退货收件信息")
        String returnAddress,

        @Schema(description = "顾客填单号时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime returnTime,

        @Schema(description = "完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime finishTime,

        @Schema(description = "申请时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
