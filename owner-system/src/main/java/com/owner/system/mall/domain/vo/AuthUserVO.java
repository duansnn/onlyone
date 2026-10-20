package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户。
 * <p>{@code role} 是全站唯一身份判断依据：{@code MERCHANT} 能进店铺后台，
 * {@code CUSTOMER} 只能逛店面。{@code platformAdmin} 保留给旧前端做兼容。</p>
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

        @Schema(description = "角色：MERCHANT 店主，CUSTOMER 顾客")
        String role,

        @Schema(description = "角色中文名")
        String roleLabel,

        @Schema(description = "是否平台超级管理员（旧字段，等价于 role=MERCHANT）")
        boolean platformAdmin
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
