package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户。
 */
@Schema(description = "当前登录用户")
public record AuthUserVO(

        @Schema(description = "用户ID")
        Long id,

        @Schema(description = "登录名")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "手机号")
        String mobile,

        @Schema(description = "邮箱")
        String email,

        @Schema(description = "头像")
        String avatarUrl,

        @Schema(description = "是否平台超级管理员")
        boolean platformAdmin
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
