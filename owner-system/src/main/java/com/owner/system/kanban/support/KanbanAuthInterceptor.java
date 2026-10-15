package com.owner.system.kanban.support;

import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 看板登录拦截器：校验 Authorization: Bearer &lt;jwt&gt;，通过后写入 ThreadLocal。
 * <p>校验失败抛 {@link ServiceException}(401)，由全局异常处理器统一转 R。</p>
 */
public class KanbanAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    public KanbanAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "未登录或令牌缺失");
        }
        String raw = authorization.substring(BEARER_PREFIX.length()).trim();
        Long userId = jwtTokenService.parseUserId(raw);
        if (userId == null) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "令牌无效或已过期，请重新登录");
        }
        KanbanCurrentUser.set(userId, jwtTokenService.parseUsername(raw));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        KanbanCurrentUser.clear();
    }
}
