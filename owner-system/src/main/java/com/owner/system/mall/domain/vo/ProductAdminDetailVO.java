package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台商品编辑详情。
 */
@Schema(description = "后台商品编辑详情")
public record ProductAdminDetailVO(

        @Schema(description = "商品ID")
        Long id,

        @Schema(description = "类目ID")
        Long categoryId,

        @Schema(description = "商品名")
        String name,

        @Schema(description = "卖点")
        String sellingPoint,

        @Schema(description = "详情图文")
        String detail,

        @Schema(description = "上架状态")
        String status,

        @Schema(description = "状态中文名")
        String statusLabel,

        @Schema(description = "排序")
        Integer sort,

        @Schema(description = "主图，第一张是封面")
        List<String> images,

        @Schema(description = "规格列表")
        List<SkuVO> skus,

        @Schema(description = "版本号（乐观锁）")
        Integer version,

        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime updateTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
