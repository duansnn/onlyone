package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 店面前台首页。
 */
@Schema(description = "店面前台首页")
public record StoreHomeVO(

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "店名")
        String shopName,

        @Schema(description = "简介")
        String intro,

        @Schema(description = "公告，没有公告时为 null，前端不显示该块")
        String notice,

        @Schema(description = "店招图")
        String bannerUrl,

        @Schema(description = "营业时间说明")
        String businessHours,

        @Schema(description = "五个一级类目入口")
        List<CategoryVO> categories,

        @Schema(description = "第一批在售商品（12 个）")
        List<ProductCardVO> products,

        @Schema(description = "在售商品总数")
        long productTotal
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
