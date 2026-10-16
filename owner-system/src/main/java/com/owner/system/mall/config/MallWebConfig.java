package com.owner.system.mall.config;

import com.owner.system.mall.support.MallAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 电商接口的拦截器注册。
 *
 * <p>受保护前缀：认证 / 平台 / 店铺 / 类目商品 / 库存 / 购物车 / 地址 / 订单 / 售后 / 评价 / 优惠券。
 * 店面（{@code /api/storefront/**}）浏览类接口公开，注册与登录公开。</p>
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class MallWebConfig implements WebMvcConfigurer {

    /** 需要登录才能访问的路径 */
    private static final String[] PROTECTED_PATTERNS = {
            "/api/auth/**",
            "/api/platform/**",
            "/api/shop/**",
            "/api/cart/**",
            "/api/address/**",
            "/api/order/**",
            "/api/pay/**",
            "/api/refund/**",
            "/api/review/**",
            "/api/coupon/**"
    };

    /** 公开路径：店面浏览、注册、登录、支付异步通知 */
    private static final String[] PUBLIC_PATTERNS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/storefront/**",
            "/api/pay/notify/**"
    };

    private final MallAuthInterceptor mallAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration registration = registry.addInterceptor(mallAuthInterceptor)
                .addPathPatterns(PROTECTED_PATTERNS)
                .excludePathPatterns(PUBLIC_PATTERNS);
        registration.order(10);
    }
}
