package com.owner.system.mall.support;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器：从 {@code Authorization: Bearer <token>} 解析登录态并写入
 * {@link MallCurrentUser}，请求结束后清理。
 * <p>只负责「你是谁」；「你能不能改这家店」由 {@code ShopGuard} 在业务层判断。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放过
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = resolveToken(request);
        JwtTokenService.TokenPayload payload = jwtTokenService.parse(token);
        if (tokenBlacklistService.isRevoked(payload.jti())) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }

        MallCurrentUser.set(new MallCurrentUser.Principal(
                payload.userId(), payload.username(), payload.platformAdmin(),
                payload.jti(), payload.expiresAtEpochSecond()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        MallCurrentUser.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.HEADER_AUTHORIZATION);
        if (header == null || header.isBlank()) {
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        }
        String value = header.trim();
        if (value.regionMatches(true, 0, Constants.TOKEN_PREFIX, 0, Constants.TOKEN_PREFIX.length())) {
            value = value.substring(Constants.TOKEN_PREFIX.length()).trim();
        }
        if (value.isEmpty()) {
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        }
        return value;
    }
}
