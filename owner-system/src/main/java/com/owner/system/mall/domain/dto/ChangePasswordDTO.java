package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码入参：必须填当前密码。
 */
@Schema(description = "修改密码入参")
public record ChangePasswordDTO(

        @Schema(description = "当前密码")
        @NotBlank(message = "当前密码不能为空")
        String currentPassword,

        @Schema(description = "新密码，6~64 位")
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "新密码长度需在 6~64 之间")
        String newPassword
) {
}
