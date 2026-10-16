package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台超管看的用户行。
 * <p>手机号在列表里中间四位打星（PRD §14），要看全号请去用户详情。</p>
 */
@Schema(description = "平台-用户")
public record PlatformUserVO(

        @Schema(description = "用户ID")
        Long id,

        @Schema(description = "登录账号")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "手机号（中间四位打星）")
        String mobileMasked,

        @Schema(description = "邮箱")
        String email,

        @Schema(description = "是否平台管理员")
        boolean platformAdmin,

        @Schema(description = "状态：0 正常，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "是否已设置密码")
        boolean passwordSet,

        @Schema(description = "注册时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
