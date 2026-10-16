package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 店主添加店员入参。手机号对应的用户不存在时按初始密码新建。
 */
@Schema(description = "店主添加店员入参")
public record MemberCreateDTO(

        @Schema(description = "手机号", example = "13800000005")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Schema(description = "初始密码，6~64 位（用户已存在时可空）")
        @Size(min = 6, max = 64, message = "初始密码长度需在 6~64 之间")
        String password,

        @Schema(description = "昵称（新建用户时使用）")
        @Size(max = 32, message = "昵称长度不能超过 32")
        String nickname,

        @Schema(description = "角色：OWNER 店主，STAFF 店员", example = "STAFF")
        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "OWNER|STAFF", message = "角色只能是 OWNER 或 STAFF")
        String role
) {
}
