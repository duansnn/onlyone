package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 下单时的一行商品。
 * <p>{@code price} 是顾客在确认页看到的单价（分），用于二次校验价格是否变化；
 * 可不传，传了就以服务端当前 SKU 价格为准做比对。</p>
 */
@Schema(description = "下单商品行")
public record OrderSkuDTO(

        @Schema(description = "规格ID")
        @NotNull(message = "规格ID不能为空")
        Long skuId,

        @Schema(description = "数量，单次不超过 99", example = "1")
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单次数量不能超过 99")
        Integer quantity,

        @Schema(description = "顾客看到的单价（分），用于价格变化校验")
        Long price
) {
}
