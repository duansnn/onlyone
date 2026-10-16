package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺成员。店员名单只显示本店，koko 不出现在店员名单里。
 */
@Schema(description = "店铺成员")
public record ShopMemberVO(

        @Schema(description = "成员记录ID")
        Long id,

        @Schema(description = "用户ID")
        Long userId,

        @Schema(description = "登录名")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "手机号")
        String mobile,

        @Schema(description = "角色：OWNER 店主，STAFF 店员")
        String role,

        @Schema(description = "角色中文名")
        String roleLabel,

        @Schema(description = "状态：0 正常，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "加入时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
