package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 提交订单入参。金额由服务端计算，前端不传。
 */
@Schema(description = "提交订单入参")
public record OrderSubmitDTO(

        @Schema(description = "店铺ID")
        @NotNull(message = "店铺ID不能为空")
        Long shopId,

        @Schema(description = "下单的行")
        @NotEmpty(message = "请先勾选商品")
        @Size(max = 100, message = "一次最多结算 100 行")
        List<@Valid OrderSkuDTO> items,

        @Schema(description = "收货地址ID")
        @NotNull(message = "请选择收货地址")
        Long addressId,

        @Schema(description = "使用的优惠券领用记录ID，可空")
        Long couponUserId,

        @Schema(description = "买家留言，200 字以内")
        @Size(max = 200, message = "买家留言不能超过 200 字")
        String buyerRemark
) {
}
