package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 店铺经营分析（数据大盘）。
 */
@Schema(description = "店铺经营分析")
public record ShopAnalyticsVO(

        @Schema(description = "近 30 天销售趋势（按天）")
        List<DailyPoint> trend,

        @Schema(description = "热销商品 Top10")
        List<TopProduct> topProducts,

        @Schema(description = "类目销售额占比")
        List<CategoryPoint> categorySales
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 单日销售点 */
    @Schema(description = "单日销售")
    public record DailyPoint(
            @Schema(description = "日期（yyyy-MM-dd）") String day,
            @Schema(description = "订单数") long orderCount,
            @Schema(description = "支付金额（分）") long paidAmount
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /** 热销商品 */
    @Schema(description = "热销商品")
    public record TopProduct(
            @Schema(description = "商品名") String productName,
            @Schema(description = "销量") long quantity,
            @Schema(description = "销售额（分）") long amount
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /** 类目销售占比 */
    @Schema(description = "类目销售")
    public record CategoryPoint(
            @Schema(description = "类目名") String categoryName,
            @Schema(description = "销售额（分）") long amount
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
