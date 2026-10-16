package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态记录。
 */
@Schema(description = "订单状态记录")
public record OrderLogVO(

        @Schema(description = "记录ID")
        Long id,

        @Schema(description = "原状态")
        String fromStatus,

        @Schema(description = "原状态中文名")
        String fromStatusLabel,

        @Schema(description = "新状态")
        String toStatus,

        @Schema(description = "新状态中文名")
        String toStatusLabel,

        @Schema(description = "操作方")
        String operatorType,

        @Schema(description = "操作方中文名")
        String operatorTypeLabel,

        @Schema(description = "操作人ID")
        Long operatorId,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
