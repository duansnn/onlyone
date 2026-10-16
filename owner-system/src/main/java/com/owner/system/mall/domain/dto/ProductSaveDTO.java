package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 保存商品入参（SPU + 主图 + SKU 一次提交）。
 */
@Schema(description = "保存商品入参")
public record ProductSaveDTO(

        @Schema(description = "二级类目ID")
        @NotNull(message = "类目不能为空")
        Long categoryId,

        @Schema(description = "名称，2~60 字", example = "抽取式厨房纸巾 3 层")
        @NotBlank(message = "商品名称不能为空")
        @Size(min = 2, max = 60, message = "商品名称长度需在 2~60 之间")
        String name,

        @Schema(description = "卖点，200 字以内")
        @Size(max = 200, message = "卖点长度不能超过 200")
        String sellingPoint,

        @Schema(description = "详情图文")
        String detail,

        @Schema(description = "上架状态：DRAFT 草稿，ON_SALE 在售，OFF_SHELF 已下架", example = "DRAFT")
        @Pattern(regexp = "^$|DRAFT|ON_SALE|OFF_SHELF", message = "状态只能是 DRAFT / ON_SALE / OFF_SHELF")
        String status,

        @Schema(description = "排序，越小越靠前", example = "0")
        Integer sort,

        @Schema(description = "主图地址，第一张是封面，最多 8 张")
        @Size(min = 1, max = 8, message = "主图需 1~8 张")
        List<@NotBlank(message = "图片地址不能为空") String> images,

        @Schema(description = "规格列表，至少 1 条")
        @NotNull(message = "规格不能为空")
        @Size(min = 1, message = "至少 1 条规格")
        List<@Valid SkuSaveDTO> skus,

        @Schema(description = "版本号（乐观锁，修改时原样回传）")
        Integer version
) {
}
