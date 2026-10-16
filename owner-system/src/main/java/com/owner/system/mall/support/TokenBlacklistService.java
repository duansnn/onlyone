package com.owner.system.mall.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 令牌黑名单：退出登录后把当前令牌写入 Redis，其余有效期内一律拒绝。
 * <p>黑名单 TTL 与令牌剩余有效期一致，到期自动清理，不会无限增长。</p>
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "owner:mall:jwt:revoked:";

    private final StringRedisTemplate redisTemplate;

    /** 拉黑一个令牌编号 */
    public void revoke(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    /** 是否已被拉黑 */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
