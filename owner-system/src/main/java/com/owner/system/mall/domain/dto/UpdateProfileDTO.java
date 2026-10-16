package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 修改个人资料入参：可改昵称、头像、邮箱。
 */
@Schema(description = "修改个人资料入参")
public record UpdateProfileDTO(

        @Schema(description = "昵称", example = "小林")
        @Size(max = 32, message = "昵称长度不能超过 32")
        String nickname,

        @Schema(description = "邮箱", example = "lin@example.com")
        @Email(message = "邮箱格式不正确")
        String email,

        @Schema(description = "头像地址")
        @Size(max = 255, message = "头像地址长度不能超过 255")
        String avatarUrl
) {
}
