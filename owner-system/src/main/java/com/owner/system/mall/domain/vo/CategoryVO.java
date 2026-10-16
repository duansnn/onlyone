package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 类目（两级，children 只在一级节点上有值）。
 */
@Schema(description = "类目")
public record CategoryVO(

        @Schema(description = "类目ID")
        Long id,

        @Schema(description = "父类目，一级为 0")
        Long parentId,

        @Schema(description = "类目名")
        String name,

        @Schema(description = "排序")
        Integer sort,

        @Schema(description = "状态：0 启用，1 停用")
        Integer status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "子类目")
        List<CategoryVO> children
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
