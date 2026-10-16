package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 平台超管重置用户密码入参。
 */
@Schema(description = "重置用户密码入参")
public record ResetPasswordDTO(

        @Schema(description = "新密码，6~64 位")
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6~64 之间")
        String password
) {
}
