package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 店面商品卡片（首页 / 列表 / 搜索同一套）。
 */
@Schema(description = "店面商品卡片")
public record ProductCardVO(

        @Schema(description = "商品ID")
        Long id,

        @Schema(description = "商品名")
        String name,

        @Schema(description = "卖点")
        String sellingPoint,

        @Schema(description = "封面图")
        String coverImage,

        @Schema(description = "最低售价（分）")
        long minPrice,

        @Schema(description = "最高售价（分）")
        long maxPrice,

        @Schema(description = "可售合计，0 表示暂时缺货")
        long totalAvailable
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
