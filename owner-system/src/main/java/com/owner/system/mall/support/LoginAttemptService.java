package com.owner.system.mall.support;

import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败计数与锁定：连续失败 5 次锁定 15 分钟。
 * <p>计数走 Redis，多实例共享；登录成功即清零。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String KEY_PREFIX = "owner:mall:login:fail:";

    /** 允许的连续失败次数 */
    private static final int MAX_FAILURES = 5;

    /** 锁定窗口 */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    /** 锁定中则抛 403，提示剩余分钟数 */
    public void assertNotLocked(String account) {
        String key = KEY_PREFIX + account;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return;
        }
        int failures;
        try {
            failures = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            redisTemplate.delete(key);
            return;
        }
        if (failures < MAX_FAILURES) {
            return;
        }
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long minutes = ttl == null || ttl <= 0 ? 1 : (ttl + 59) / 60;
        throw new ServiceException(ResultCode.FORBIDDEN,
                "账号已锁定，请 " + minutes + " 分钟后再试");
    }

    /** 记录一次失败；首次失败时启动 15 分钟窗口 */
    public void onFailure(String account) {
        String key = KEY_PREFIX + account;
        Long failures = redisTemplate.opsForValue().increment(key);
        if (failures != null && failures == 1L) {
            redisTemplate.expire(key, LOCK_DURATION);
        }
    }

    /** 登录成功清零 */
    public void onSuccess(String account) {
        redisTemplate.delete(KEY_PREFIX + account);
    }
}
