package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设置 SKU 安全库存阈值。safeStock 传 null 表示取消预警。
 */
@Schema(description = "设置安全库存阈值请求")
public record StockSafeDTO(

        @Schema(description = "规格 ID")
        @NotNull(message = "规格 ID 不能为空")
        Long skuId,

        @Schema(description = "安全库存阈值，可售 <= 该值即预警；null 表示取消预警")
        Integer safeStock
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
