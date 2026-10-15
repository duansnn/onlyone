package com.owner.system.kanban.support;

import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;

/**
 * 看板当前登录用户上下文（ThreadLocal，由 KanbanAuthInterceptor 填充/清理）。
 */
public final class KanbanCurrentUser {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    private KanbanCurrentUser() {
    }

    public static void set(Long userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    /** 获取当前用户 id；未登录（拦截器未覆盖的路径误调用）时直接抛 401。 */
    public static Long getRequiredUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}
