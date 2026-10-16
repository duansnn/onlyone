package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 我的购物车。
 */
@Schema(description = "我的购物车")
public record CartVO(

        @Schema(description = "按店铺分组")
        List<CartShopGroupVO> shops,

        @Schema(description = "行数合计")
        long itemCount,

        @Schema(description = "可结算行数")
        long validItemCount
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
