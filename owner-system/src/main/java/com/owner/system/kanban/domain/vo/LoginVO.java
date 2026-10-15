package com.owner.system.kanban.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录成功返回：JWT 令牌 + 基本用户信息。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT 令牌，前端存 localStorage，后续请求带 Authorization: Bearer xxx */
    private String token;

    /** 用户 id */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 令牌过期时间（epoch 毫秒） */
    private Long expireAt;
}
