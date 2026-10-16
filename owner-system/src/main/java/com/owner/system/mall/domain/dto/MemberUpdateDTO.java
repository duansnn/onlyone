package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * 店主变更店员角色 / 启用停用入参。两个字段都可空，只改传了的那个。
 */
@Schema(description = "店主变更店员入参")
public record MemberUpdateDTO(

        @Schema(description = "角色：OWNER 店主，STAFF 店员")
        @Pattern(regexp = "^$|OWNER|STAFF", message = "角色只能是 OWNER 或 STAFF")
        String role,

        @Schema(description = "状态：0 正常，1 停用")
        @Pattern(regexp = "^$|0|1", message = "状态只能是 0 或 1")
        String status
) {
}
