package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 修改手机号入参：手机号需单独验证后再改。
 */
@Schema(description = "修改手机号入参")
public record ChangeMobileDTO(

        @Schema(description = "发到新手机号的验证码")
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
        String code,

        @Schema(description = "新手机号", example = "13800000003")
        @NotBlank(message = "新手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile
) {
}
