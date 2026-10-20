package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 店主变更店员入参。所有字段都可空，只改传了的那个。
 *
 * <p>既能改店内的角色与启停（{@code role} / {@code status}），
 * 也能改这个人的账号资料（{@code nickname} / {@code mobile} / {@code password}）。
 * 手机号是店员的登录账号，改号码时登录账号跟着换。</p>
 */
@Schema(description = "店主变更店员入参")
public record MemberUpdateDTO(

        @Schema(description = "角色：OWNER 店主，STAFF 店员")
        @Pattern(regexp = "^$|OWNER|STAFF", message = "角色只能是 OWNER 或 STAFF")
        String role,

        @Schema(description = "状态：0 正常，1 停用")
        @Pattern(regexp = "^$|0|1", message = "状态只能是 0 或 1")
        String status,

        @Schema(description = "昵称，不传则不改")
        @Size(max = 32, message = "昵称长度不能超过 32")
        String nickname,

        @Schema(description = "手机号（同时也是登录账号），不传则不改")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Schema(description = "新密码，6~64 位；留空表示不重置")
        @Size(min = 6, max = 64, message = "新密码长度需在 6~64 之间")
        String password
) {
}
