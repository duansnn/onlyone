package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 取消待付款订单入参。原因可选：不想要了、信息填错、重新下单。
 */
@Schema(description = "取消订单入参")
public record OrderCancelDTO(

        @Schema(description = "取消原因", example = "不想要了")
        @Size(max = 64, message = "取消原因不能超过 64 字")
        String reason
) {
}
