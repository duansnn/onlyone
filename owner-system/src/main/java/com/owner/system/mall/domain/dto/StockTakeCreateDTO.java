package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 新建库存盘点单。明细在「提交盘点」时再逐条登记。
 */
@Schema(description = "新建盘点单请求")
public record StockTakeCreateDTO(

        @Schema(description = "盘点单标题")
        @NotBlank(message = "盘点单标题不能为空")
        @Size(max = 64, message = "标题最长 64 字")
        String title,

        @Schema(description = "备注")
        @Size(max = 255, message = "备注最长 255 字")
        String remark
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
