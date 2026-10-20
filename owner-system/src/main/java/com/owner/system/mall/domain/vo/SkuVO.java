package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品规格。可售 = 库存 − 锁定量。
 */
@Schema(description = "商品规格 SKU")
public record SkuVO(

        @Schema(description = "规格ID")
        Long id,

        @Schema(description = "规格名")
        String specName,

        @Schema(description = "售价（分）")
        Long price,

        @Schema(description = "划线价（分）。顾客端不下发，只有后台商品编辑能看到")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long listPrice,

        @Schema(description = "库存")
        Integer stock,

        @Schema(description = "锁定量")
        Integer lockedStock,

        @Schema(description = "可售库存")
        Integer availableStock,

        @Schema(description = "货号")
        String skuCode,

        @Schema(description = "重量（克）")
        Integer weightGram,

        @Schema(description = "状态：0 启用，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
