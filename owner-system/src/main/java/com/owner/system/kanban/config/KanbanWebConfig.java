package com.owner.system.kanban.config;

import com.owner.system.kanban.support.JwtTokenService;
import com.owner.system.kanban.support.KanbanAuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 看板模块 Web 配置：登录拦截 + BCrypt 编码器。
 * <p>只拦截看板相关路径，门户既有接口（/api/portal/**）不受影响。</p>
 */
@Configuration(proxyBeanMethods = false)
public class KanbanWebConfig implements WebMvcConfigurer {

    private final JwtTokenService jwtTokenService;

    public KanbanWebConfig(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new KanbanAuthInterceptor(jwtTokenService))
                .addPathPatterns("/api/user/**", "/api/project/**", "/api/task/**", "/api/ai/**",
                        "/api/stats/**", "/api/note/**", "/api/focus/**", "/api/article/**")
                .excludePathPatterns("/api/user/register", "/api/user/login");
    }

    /** BCrypt 密码编码器（仅用 spring-security-crypto，不引入完整 Security 过滤链）。 */
    @Bean
    public PasswordEncoder kanbanPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
