package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存盘点单（含明细）。
 */
@Schema(description = "库存盘点单")
public record StockTakeVO(

        @Schema(description = "盘点单 ID")
        Long id,

        @Schema(description = "标题")
        String title,

        @Schema(description = "状态：DRAFT / DONE")
        String status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "明细")
        List<ItemVO> items
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "盘点明细")
    public record ItemVO(

            @Schema(description = "规格 ID")
            Long skuId,

            @Schema(description = "商品名")
            String productName,

            @Schema(description = "规格名")
            String specName,

            @Schema(description = "账面库存")
            Integer bookStock,

            @Schema(description = "实盘库存")
            Integer actualStock,

            @Schema(description = "差异（实盘-账面）")
            Integer diffQty
    ) {
    }
}
