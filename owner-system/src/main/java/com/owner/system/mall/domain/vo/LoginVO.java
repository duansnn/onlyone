package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录结果。
 * <p>前端按此路由：{@code platformAdmin} 为真 → 平台后台；
 * {@code shops} 非空 → 店铺后台（多家先选店）；否则 → 店面。</p>
 */
@Schema(description = "登录结果")
public record LoginVO(

        @Schema(description = "访问令牌")
        String token,

        @Schema(description = "令牌类型")
        String tokenType,

        @Schema(description = "有效期（秒）")
        long expireSeconds,

        @Schema(description = "当前用户")
        AuthUserVO user,

        @Schema(description = "我所在的店铺（可能为空）")
        List<ShopBriefVO> shops
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
