package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 店主修改本店资料入参。
 */
@Schema(description = "店主修改本店资料入参")
public record ShopUpdateDTO(

        @Schema(description = "店名", example = "日用铺")
        @NotBlank(message = "店名不能为空")
        @Size(min = 2, max = 64, message = "店名长度需在 2~64 之间")
        String name,

        @Schema(description = "简介，500 字以内")
        @Size(max = 500, message = "简介长度不能超过 500")
        String intro,

        @Schema(description = "公告，可空")
        @Size(max = 500, message = "公告长度不能超过 500")
        String notice,

        @Schema(description = "联系人")
        @Size(max = 32, message = "联系人长度不能超过 32")
        String contactName,

        @Schema(description = "联系电话")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String contactMobile,

        @Schema(description = "发货地址（售后退货默认用它）")
        @Size(max = 255, message = "发货地址长度不能超过 255")
        String shipAddress,

        @Schema(description = "店招图地址")
        @Size(max = 255, message = "店招图地址长度不能超过 255")
        String bannerUrl,

        @Schema(description = "营业时间说明")
        @Size(max = 64, message = "营业时间说明长度不能超过 64")
        String businessHours,

        @Schema(description = "收款说明")
        @Size(max = 255, message = "收款说明长度不能超过 255")
        String payNote,

        @Schema(description = "版本号（乐观锁，原样回传）")
        Integer version
) {
}
