package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收货地址。
 */
@Schema(description = "收货地址")
public record AddressVO(

        @Schema(description = "地址ID")
        Long id,

        @Schema(description = "收件人")
        String receiver,

        @Schema(description = "手机")
        String mobile,

        @Schema(description = "省")
        String province,

        @Schema(description = "市")
        String city,

        @Schema(description = "区")
        String district,

        @Schema(description = "详细地址")
        String detail,

        @Schema(description = "拼接后的完整地址")
        String fullAddress,

        @Schema(description = "是否默认：0 否，1 是")
        Integer isDefault
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 拼接省市区与详细地址 */
    public static String join(String province, String city, String district, String detail) {
        // 直辖市与港澳的省市同名（北京市 / 北京市 / 朝阳区），相邻层级重复时只保留一次，
        // 否则会拼成「北京市北京市朝阳区」
        StringBuilder sb = new StringBuilder();
        String prev = null;
        for (String part : new String[]{province, city, district, detail}) {
            if (part == null || part.isBlank() || part.equals(prev)) {
                continue;
            }
            sb.append(part);
            prev = part;
        }
        return sb.toString();
    }
}
