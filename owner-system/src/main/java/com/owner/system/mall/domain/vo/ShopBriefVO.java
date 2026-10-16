package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 我所在的店铺（用于登录后决定进哪一个后台，以及多店时选店）。
 */
@Schema(description = "我所在的店铺")
public record ShopBriefVO(

        @Schema(description = "店铺ID")
        Long shopId,

        @Schema(description = "店名")
        String shopName,

        @Schema(description = "角色：OWNER 店主，STAFF 店员")
        String role,

        @Schema(description = "角色中文名")
        String roleLabel
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
