package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 购物车改数量入参。
 */
@Schema(description = "购物车改数量入参")
public record CartQuantityDTO(

        @Schema(description = "规格ID")
        @NotNull(message = "规格ID不能为空")
        Long skuId,

        @Schema(description = "数量，0 表示删除该行，最大 99", example = "2")
        @NotNull(message = "数量不能为空")
        @Min(value = 0, message = "数量不能为负")
        @Max(value = 99, message = "数量不能超过 99")
        Integer quantity
) {
}
