package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 确认订单页计价入参（不落库，只算钱）。
 */
@Schema(description = "确认订单页计价入参")
public record OrderPreviewDTO(

        @Schema(description = "店铺ID")
        @NotNull(message = "店铺ID不能为空")
        Long shopId,

        @Schema(description = "勾选的行")
        @NotEmpty(message = "请先勾选商品")
        @Size(max = 100, message = "一次最多结算 100 行")
        List<@Valid OrderSkuDTO> items,

        @Schema(description = "想用的优惠券领用记录ID，可空")
        Long couponUserId
) {
}
