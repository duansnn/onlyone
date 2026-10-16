package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 保存收货地址入参。{@code id} 为空表示新增。
 */
@Schema(description = "保存收货地址入参")
public record AddressSaveDTO(

        @Schema(description = "地址ID，新增时留空")
        Long id,

        @Schema(description = "收件人", example = "小林")
        @NotBlank(message = "收件人不能为空")
        @Size(max = 32, message = "收件人长度不能超过 32")
        String receiver,

        @Schema(description = "手机", example = "13800000002")
        @NotBlank(message = "手机不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Schema(description = "省", example = "广东省")
        @Size(max = 32, message = "省长度不能超过 32")
        String province,

        @Schema(description = "市", example = "深圳市")
        @Size(max = 32, message = "市长度不能超过 32")
        String city,

        @Schema(description = "区", example = "南山区")
        @Size(max = 32, message = "区长度不能超过 32")
        String district,

        @Schema(description = "详细地址", example = "科技园 1 号 101 室")
        @NotBlank(message = "详细地址不能为空")
        @Size(max = 255, message = "详细地址长度不能超过 255")
        String detail,

        @Schema(description = "是否默认：true 设为默认（会取消原默认）")
        Boolean isDefault
) {
}
