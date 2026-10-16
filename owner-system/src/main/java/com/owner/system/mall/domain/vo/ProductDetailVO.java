package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 店面商品详情。
 */
@Schema(description = "店面商品详情")
public record ProductDetailVO(

        @Schema(description = "商品ID")
        Long id,

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "商品名")
        String name,

        @Schema(description = "卖点")
        String sellingPoint,

        @Schema(description = "详情图文")
        String detail,

        @Schema(description = "主图，第一张是封面")
        List<String> images,

        @Schema(description = "规格列表（含停用的，前端自行置灰）")
        List<SkuVO> skus,

        @Schema(description = "最低售价（分）")
        long minPrice,

        @Schema(description = "最高售价（分）")
        long maxPrice,

        @Schema(description = "可售合计")
        long totalAvailable,

        @Schema(description = "评价条数")
        long reviewCount,

        @Schema(description = "平均分，无评价时为 0")
        double averageScore
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
