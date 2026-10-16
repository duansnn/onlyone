package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 顾客填写退货运单号入参。
 */
@Schema(description = "填写退货运单号入参")
public record RefundReturnShipDTO(

        @Schema(description = "退货快递公司", example = "中通")
        @NotBlank(message = "请选择快递公司")
        @Size(max = 32, message = "快递公司长度不能超过 32")
        String returnCompany,

        @Schema(description = "退货运单号，6~40 位", example = "ZT1234567890")
        @NotBlank(message = "退货运单号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9-]{6,40}$", message = "退货运单号需为 6~40 位数字或字母")
        String returnShipNo
) {
}
