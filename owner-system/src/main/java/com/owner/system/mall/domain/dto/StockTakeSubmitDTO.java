package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 提交盘点：对每个 SKU 填实盘数，系统按「实盘 - 账面」生成差异调整。
 */
@Schema(description = "提交盘点请求")
public record StockTakeSubmitDTO(

        @Schema(description = "盘点明细（每个 SKU 一条实盘数）")
        @NotEmpty(message = "盘点明细不能为空")
        @Valid
        List<Item> items
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "单条盘点明细")
    public record Item(

            @Schema(description = "规格 ID")
            @NotNull(message = "规格 ID 不能为空")
            Long skuId,

            @Schema(description = "实盘库存")
            @NotNull(message = "实盘库存不能为空")
            Integer actualStock
    ) {
    }
}
