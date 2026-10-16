package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交订单结果。
 */
@Schema(description = "提交订单结果")
public record OrderSubmitVO(

        @Schema(description = "订单ID")
        Long orderId,

        @Schema(description = "订单号")
        String orderNo,

        @Schema(description = "应付（分）")
        long payAmount,

        @Schema(description = "待付款超时时间，超过会自动取消并释放库存")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime expireTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
