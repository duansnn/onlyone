package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 后台「顾客管理」抽屉详情：基础档案 + 消费统计 + 收货地址 + 最近订单。
 */
@Schema(description = "顾客详情")
public record CustomerDetailVO(

        @Schema(description = "基础档案与消费统计")
        PlatformUserVO profile,

        @Schema(description = "收货地址（默认地址在前）")
        List<AddressVO> addresses,

        @Schema(description = "最近订单（最多 20 条，新的在前）")
        List<CustomerOrderVO> orders
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
