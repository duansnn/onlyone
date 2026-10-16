package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 申请售后入参。未发货只能「仅退款」，已发货或已完成 7 天内只能「退货退款」。
 */
@Schema(description = "申请售后入参")
public record RefundApplyDTO(

        @Schema(description = "REFUND_ONLY 仅退款，RETURN_REFUND 退货退款", example = "REFUND_ONLY")
        @NotBlank(message = "售后类型不能为空")
        @Pattern(regexp = "REFUND_ONLY|RETURN_REFUND", message = "售后类型只能是 REFUND_ONLY 或 RETURN_REFUND")
        String type,

        @Schema(description = "原因", example = "不想要了")
        @NotBlank(message = "原因不能为空")
        @Size(max = 64, message = "原因不能超过 64 字")
        String reason,

        @Schema(description = "补充说明，300 字以内")
        @Size(max = 300, message = "说明不能超过 300 字")
        String description
) {
}
