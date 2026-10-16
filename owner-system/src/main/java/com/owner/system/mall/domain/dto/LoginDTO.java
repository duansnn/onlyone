package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录入参。账号可以是登录名，也可以是手机号。
 */
@Schema(description = "登录入参")
public record LoginDTO(

        @Schema(description = "账号（登录名或手机号）", example = "shopadmin")
        @NotBlank(message = "账号不能为空")
        String account,

        @Schema(description = "密码", example = "shop123")
        @NotBlank(message = "密码不能为空")
        String password
) {
}
