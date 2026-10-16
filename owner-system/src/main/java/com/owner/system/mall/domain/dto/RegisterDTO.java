package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 店面注册入参：账号用手机号，密码 6~64 位，注册成功即登录，身份是顾客。
 */
@Schema(description = "店面注册入参")
public record RegisterDTO(

        @Schema(description = "手机号（同时作为登录账号）", example = "13800000002")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Schema(description = "密码，6~64 位", example = "a123456")
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6~64 之间")
        String password,

        @Schema(description = "昵称，可空", example = "小林")
        @Size(max = 32, message = "昵称长度不能超过 32")
        String nickname
) {
}
