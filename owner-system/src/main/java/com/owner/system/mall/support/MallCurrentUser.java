package com.owner.system.mall.support;

import com.owner.common.core.domain.R;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

/**
 * 当前登录用户上下文（线程级）。
 * <p>由 {@link MallAuthInterceptor} 在请求进入时写入，请求结束时清理。</p>
 */
@Slf4j
public final class MallCurrentUser {

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    private MallCurrentUser() {
    }

    /** 登录态快照 */
    public record Principal(Long userId, String username, boolean platformAdmin,
                            String jti, long expiresAtEpochSecond) {
    }

    static void set(Principal principal) {
        HOLDER.set(principal);
    }

    static void clear() {
        HOLDER.remove();
    }

    /** 未登录返回 null */
    public static Principal getOrNull() {
        return HOLDER.get();
    }

    /** 取当前用户，未登录抛 401 */
    public static Principal require() {
        Principal principal = HOLDER.get();
        if (principal == null) {
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        }
        return principal;
    }

    /** 取当前用户 ID，未登录抛 401 */
    public static Long requireUserId() {
        return require().userId();
    }

    /** 是否为平台超级管理员（koko） */
    public static boolean isPlatformAdmin() {
        Principal principal = HOLDER.get();
        return principal != null && principal.platformAdmin();
    }

    /**
     * 是否为店主（商家）。
     * <p>单商家模式：店主就是 {@code platform_admin = 1} 的那个账号；
     * 顾客的令牌里这个标记恒为 false，因此碰不到任何经营接口。</p>
     */
    public static boolean isMerchant() {
        return isPlatformAdmin();
    }

    /** 校验店主身份，否则抛 403。经营类接口（商品、订单、顾客、店铺资料）都走这里。 */
    public static void requireMerchant() {
        Principal principal = require();
        if (!principal.platformAdmin()) {
            log.warn("[MallAuth] 顾客尝试访问经营接口 userId={}", principal.userId());
            throw new ServiceException(ResultCode.FORBIDDEN, "仅店主可操作");
        }
    }

    /** 旧的平台管理员校验，等价于 {@link #requireMerchant()} */
    public static void requirePlatformAdmin() {
        requireMerchant();
    }

    /** 便于日志输出，不抛异常 */
    public static String describe() {
        Principal principal = HOLDER.get();
        return principal == null
                ? "anonymous"
                : principal.userId() + "/" + principal.username();
    }

    /** 供测试或异步透传使用 */
    public static R<Void> unauthorized() {
        return R.fail(ResultCode.UNAUTHORIZED);
    }
}
