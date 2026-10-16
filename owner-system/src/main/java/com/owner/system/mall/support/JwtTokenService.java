package com.owner.system.mall.support;

import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 自实现的 HS256 JWT：只用 JDK 的 {@link Mac} 与 Base64，
 * 不引入 jjwt / java-jwt（它们在 Spring Boot 4 下会带进 Jackson 2，和 Jackson 3 冲突）。
 *
 * <p>令牌结构：{@code base64url(header).base64url(payload).base64url(signature)}</p>
 * <p>载荷字段：{@code sub} 用户 ID、{@code uname} 账号、{@code adm} 是否平台管理员、
 * {@code jti} 令牌唯一编号（用于退出登录时拉黑）、{@code iat} / {@code exp} 秒级时间戳。</p>
 */
@Slf4j
@Component
public class JwtTokenService {

    /** 固定头部（alg=HS256, typ=JWT） */
    private static final String ENCODED_HEADER = base64Url(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private static final String HMAC_SHA256 = "HmacSHA256";

    /** 生产环境务必通过 OWNER_JWT_SECRET 覆盖 */
    private static final String DEFAULT_SECRET =
            "owner-mall-dev-secret-please-override-in-production";

    private final byte[] secret;
    private final long expireSeconds;

    public JwtTokenService(
            @Value("${owner.auth.jwt.secret:" + DEFAULT_SECRET + "}") String secret,
            @Value("${owner.auth.jwt.expire-seconds:604800}") long expireSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireSeconds = expireSeconds;
        if (DEFAULT_SECRET.equals(secret)) {
            log.warn("[JWT] 正在使用内置开发密钥，生产环境请设置 owner.auth.jwt.secret");
        }
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 签发令牌。
     *
     * @param userId        用户 ID
     * @param username      登录账号
     * @param platformAdmin 是否平台超级管理员
     */
    public String createToken(Long userId, String username, boolean platformAdmin) {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("uname", username);
        payload.put("adm", platformAdmin ? 1 : 0);
        payload.put("jti", UUID.randomUUID().toString().replace("-", ""));
        payload.put("iat", now);
        payload.put("exp", now + expireSeconds);

        String body = base64Url(JsonUtils.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String signingInput = ENCODED_HEADER + "." + body;
        return signingInput + "." + base64Url(sign(signingInput));
    }

    /**
     * 校验并解析令牌。
     *
     * @throws ServiceException 令牌缺失、格式错误、签名不符或已过期（401）
     */
    public TokenPayload parse(String token) {
        if (token == null || token.isBlank()) {
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录令牌格式不正确");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = sign(signingInput);
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录令牌格式不正确");
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录令牌校验失败");
        }

        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录令牌格式不正确");
        }
        Map<String, Object> payload = JsonUtils.parseMap(json);
        if (payload == null || payload.isEmpty()) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录令牌内容无法解析");
        }

        long exp = asLong(payload.get("exp"), 0L);
        long now = System.currentTimeMillis() / 1000L;
        if (exp <= now) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        String subject = String.valueOf(payload.get("sub"));
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "登录令牌内容无法解析");
        }
        String username = payload.get("uname") == null ? null : String.valueOf(payload.get("uname"));
        boolean admin = asLong(payload.get("adm"), 0L) == 1L;
        String jti = payload.get("jti") == null ? null : String.valueOf(payload.get("jti"));

        return new TokenPayload(userId, username, admin, jti, exp);
    }

    /** 令牌剩余有效秒数，用于退出登录时设置黑名单 TTL */
    public long remainingSeconds(TokenPayload payload) {
        return Math.max(0L, payload.expiresAtEpochSecond() - System.currentTimeMillis() / 1000L);
    }

    /** 解析后的载荷 */
    public record TokenPayload(Long userId, String username, boolean platformAdmin,
                               String jti, long expiresAtEpochSecond) {
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new ServiceException(ResultCode.ERROR, "登录令牌签发失败");
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static long asLong(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
