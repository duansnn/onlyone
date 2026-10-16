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
        StringBuilder sb = new StringBuilder();
        if (province != null) {
            sb.append(province);
        }
        if (city != null) {
            sb.append(city);
        }
        if (district != null) {
            sb.append(district);
        }
        if (detail != null) {
            sb.append(detail);
        }
        return sb.toString();
    }
}
