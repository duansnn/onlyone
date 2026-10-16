package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 购物车按店铺分组。现在只有日用铺一组，结构按多店留着。
 */
@Schema(description = "购物车店铺分组")
public record CartShopGroupVO(

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "店名")
        String shopName,

        @Schema(description = "店铺状态：0 营业，1 停用")
        Integer shopStatus,

        @Schema(description = "可结算的行")
        List<CartItemVO> items,

        @Schema(description = "失效的行：不可勾选，可删除")
        List<CartItemVO> invalidItems
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
