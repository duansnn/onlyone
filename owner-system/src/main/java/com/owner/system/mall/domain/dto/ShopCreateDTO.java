package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 平台新建店铺入参。
 * <p>店主要么给 {@code ownerUserId}（必须是已有用户），
 * 要么给 {@code ownerMobile}（找不到用户时用 {@code ownerPassword} 新建一个）。</p>
 */
@Schema(description = "平台新建店铺入参")
public record ShopCreateDTO(

        @Schema(description = "店名", example = "日用铺")
        @NotBlank(message = "店名不能为空")
        @Size(min = 2, max = 64, message = "店名长度需在 2~64 之间")
        String name,

        @Schema(description = "店主用户ID（已有用户）")
        Long ownerUserId,

        @Schema(description = "店主手机号（用户不存在时按此新建）", example = "13800000001")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "店主手机号格式不正确")
        String ownerMobile,

        @Schema(description = "店主初始密码，仅在按手机号新建用户时使用")
        @Size(min = 6, max = 64, message = "初始密码长度需在 6~64 之间")
        String ownerPassword,

        @Schema(description = "店主昵称，仅在新建用户时使用")
        @Size(max = 32, message = "昵称长度不能超过 32")
        String ownerNickname,

        @Schema(description = "联系人")
        @Size(max = 32, message = "联系人长度不能超过 32")
        String contactName,

        @Schema(description = "联系电话")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String contactMobile,

        @Schema(description = "发货地址")
        @Size(max = 255, message = "发货地址长度不能超过 255")
        String shipAddress,

        @Schema(description = "简介，500 字以内")
        @Size(max = 500, message = "简介长度不能超过 500")
        String intro
) {
}
