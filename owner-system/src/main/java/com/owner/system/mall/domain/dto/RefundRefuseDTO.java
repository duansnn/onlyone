package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 店铺拒绝售后入参：必须填写理由。
 */
@Schema(description = "拒绝售后入参")
public record RefundRefuseDTO(

        @Schema(description = "拒绝理由", example = "商品已拆封影响二次销售")
        @NotBlank(message = "拒绝理由不能为空")
        @Size(max = 255, message = "拒绝理由不能超过 255 字")
        String refuseReason
) {
}
