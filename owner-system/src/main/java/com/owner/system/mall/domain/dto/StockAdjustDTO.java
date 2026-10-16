package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 库存调整入参：填调整后的数量和原因。
 */
@Schema(description = "库存调整入参")
public record StockAdjustDTO(

        @Schema(description = "规格ID")
        @NotNull(message = "规格ID不能为空")
        Long skuId,

        @Schema(description = "调整后的库存数量", example = "120")
        @NotNull(message = "库存数量不能为空")
        @Min(value = 0, message = "库存不能为负")
        Integer stock,

        @Schema(description = "原因：MANUAL 手动调整，INIT 初始入库", example = "MANUAL")
        @Pattern(regexp = "^$|MANUAL|INIT", message = "原因只能是 MANUAL 或 INIT")
        String reason,

        @Schema(description = "备注")
        @Size(max = 255, message = "备注长度不能超过 255")
        String remark
) {
}
