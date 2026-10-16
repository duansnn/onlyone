package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 购物车行入参：加购、改数量、合并本机购物车都用它。
 */
@Schema(description = "购物车行入参")
public record CartLineDTO(

        @Schema(description = "规格ID")
        @NotNull(message = "规格ID不能为空")
        Long skuId,

        @Schema(description = "数量，单次不超过 99", example = "1")
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单次数量不能超过 99")
        Integer quantity
) {
}
