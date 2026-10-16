package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺资料。
 */
@Schema(description = "店铺资料")
public record ShopVO(

        @Schema(description = "店铺ID")
        Long id,

        @Schema(description = "店名")
        String name,

        @Schema(description = "简介")
        String intro,

        @Schema(description = "公告")
        String notice,

        @Schema(description = "联系人")
        String contactName,

        @Schema(description = "联系电话")
        String contactMobile,

        @Schema(description = "发货地址")
        String shipAddress,

        @Schema(description = "店招图")
        String bannerUrl,

        @Schema(description = "营业时间说明")
        String businessHours,

        @Schema(description = "收款说明")
        String payNote,

        @Schema(description = "状态：0 营业，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "版本号（乐观锁，修改时原样回传）")
        Integer version,

        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime updateTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
