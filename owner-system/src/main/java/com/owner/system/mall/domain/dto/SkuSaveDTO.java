package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 保存 SKU 入参。{@code id} 为空表示新增规格。
 */
@Schema(description = "保存 SKU 入参")
public record SkuSaveDTO(

        @Schema(description = "规格ID，新增时留空")
        Long id,

        @Schema(description = "规格名，无多规格写「默认」", example = "默认")
        @NotBlank(message = "规格名不能为空")
        @Size(max = 64, message = "规格名长度不能超过 64")
        String specName,

        @Schema(description = "售价（分），必须大于 0", example = "990")
        @Min(value = 1, message = "售价必须大于 0")
        Long price,

        @Schema(description = "划线价（分），只展示不参与结算", example = "1290")
        @Min(value = 0, message = "划线价不能为负")
        Long listPrice,

        @Schema(description = "库存", example = "100")
        @Min(value = 0, message = "库存不能为负")
        Integer stock,

        @Schema(description = "货号，店内唯一，可空", example = "SKU-001")
        @Size(max = 64, message = "货号长度不能超过 64")
        String skuCode,

        @Schema(description = "重量（克）", example = "300")
        @Min(value = 0, message = "重量不能为负")
        Integer weightGram,

        @Schema(description = "状态：0 启用，1 停用", example = "0")
        @Pattern(regexp = "^$|0|1", message = "状态只能是 0 或 1")
        String status
) {
}
