package com.owner.system.mall.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 短信验证码下发结果。
 * <p>未接入短信通道时，{@code devCode} 会把验证码带回给调用方（仅限
 * {@code owner.mall.sms.expose-code=true}，本地联调用），生产环境请关闭。</p>
 */
@Schema(description = "验证码下发结果")
public record MobileCodeVO(

        @Schema(description = "手机号")
        String mobile,

        @Schema(description = "有效期（秒）")
        long expireSeconds,

        @Schema(description = "开发环境回显的验证码，生产环境为 null")
        String devCode
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
