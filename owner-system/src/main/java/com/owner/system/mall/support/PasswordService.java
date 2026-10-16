package com.owner.system.mall.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 口令编码：只使用 BCrypt，不引入 Spring Security 过滤器链。
 * <p>注册 / 建店员时写入 {@code password_hash}，登录时用 {@link #matches} 校验。
 * BCrypt 自带盐值，同一口令每次编码结果不同。</p>
 */
@Component
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 编码明文口令 */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /** 校验明文口令与散列是否匹配 */
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isEmpty()) {
            return false;
        }
        return encoder.matches(rawPassword, passwordHash);
    }
}
