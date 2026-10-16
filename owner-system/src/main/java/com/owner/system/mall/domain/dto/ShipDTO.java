package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 后台发货入参。
 */
@Schema(description = "后台发货入参")
public record ShipDTO(

        @Schema(description = "快递公司", example = "顺丰")
        @NotBlank(message = "请选择快递公司")
        @Size(max = 32, message = "快递公司长度不能超过 32")
        String shipCompany,

        @Schema(description = "运单号，6~40 位", example = "SF1234567890")
        @NotBlank(message = "运单号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9-]{6,40}$", message = "运单号需为 6~40 位数字或字母")
        String shipNo
) {
}
