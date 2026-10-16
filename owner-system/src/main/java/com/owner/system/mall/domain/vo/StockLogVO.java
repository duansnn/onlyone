package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水行。
 */
@Schema(description = "库存流水行")
public record StockLogVO(

        @Schema(description = "流水ID")
        Long id,

        @Schema(description = "规格ID")
        Long skuId,

        @Schema(description = "商品名")
        String productName,

        @Schema(description = "规格名")
        String specName,

        @Schema(description = "本次库存增减量")
        Integer changeQty,

        @Schema(description = "变动后库存")
        Integer afterStock,

        @Schema(description = "变动后锁定量")
        Integer afterLocked,

        @Schema(description = "原因")
        String reason,

        @Schema(description = "原因中文名")
        String reasonLabel,

        @Schema(description = "关联订单ID")
        Long orderId,

        @Schema(description = "操作人ID")
        Long operatorId,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "发生时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
