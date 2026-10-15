package com.owner.system.kanban.support;

import com.owner.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简 HS256 JWT 实现（零第三方依赖，避免 jjwt 拉入 Jackson 2 与 Boot 4 的 Jackson 3 冲突）。
 * <p>结构：base64url(header).base64url(payload).base64url(HMAC-SHA256)</p>
 * <p>密钥与有效期由 {@code owner.kanban.jwt.*} 配置。</p>
 */
@Slf4j
@Component
public class JwtTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final long expireHours;

    public JwtTokenService(@Value("${owner.kanban.jwt.secret:owner-kanban-jwt-secret-change-me-in-production}") String secret,
                           @Value("${owner.kanban.jwt.expire-hours:72}") long expireHours) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireHours = expireHours;
        log.info("[Kanban] JWT 就绪：expireHours={}h", expireHours);
    }

    /** 签发令牌。 */
    public String createToken(Long userId, String username) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("iat", now);
        payload.put("exp", now + expireHours * 3600);

        String head = b64(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String body = b64(JsonUtils.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String sig = sign(head + "." + body);
        return head + "." + body + "." + sig;
    }

    /** 令牌过期时间（epoch 毫秒）。 */
    public long expireAtMillis() {
        return System.currentTimeMillis() + expireHours * 3600_000L;
    }

    /**
     * 校验并解析令牌。
     *
     * @return 用户 id；签名不符或已过期返回 null
     */
    public Long parseUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        // 1. 验签（常量时间比较，防时序攻击）
        String expected = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        // 2. 解析 payload
        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        var node = JsonUtils.readTree(json);
        if (node == null || node.path("userId").isMissingNode()) {
            return null;
        }
        // 3. 校验过期
        long exp = node.path("exp").asLong(0);
        if (exp > 0 && Instant.now().getEpochSecond() >= exp) {
            return null;
        }
        return node.path("userId").asLong();
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            return b64(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
