package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 保存类目入参。{@code id} 为空表示新增，否则为修改。
 */
@Schema(description = "保存类目入参")
public record CategorySaveDTO(

        @Schema(description = "类目ID，新增时留空")
        Long id,

        @Schema(description = "父类目，一级给 0", example = "0")
        @NotNull(message = "父类目不能为空")
        Long parentId,

        @Schema(description = "类目名", example = "家居清洁")
        @NotBlank(message = "类目名不能为空")
        @Size(max = 32, message = "类目名长度不能超过 32")
        String name,

        @Schema(description = "排序，越小越靠前", example = "0")
        Integer sort
) {
}
