package com.owner.system.mall.config;

import com.owner.system.mall.support.MallAuthInterceptor;
import com.owner.system.mall.support.file.LocalFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 电商接口的拦截器注册 + 本地上传文件的静态映射。
 *
 * <p>受保护前缀：认证 / 平台 / 店铺 / 类目商品 / 库存 / 购物车 / 地址 / 订单 / 售后 / 评价 / 优惠券 / 上传。
 * 店面（{@code /api/storefront/**}）浏览类接口公开，注册与登录公开。</p>
 *
 * <p>上传的文件通过 {@code /files/**} 直接访问，不需要登录——图片要能在 img 标签里加载，
 * 而 img 请求不会带 Authorization 头。</p>
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
            "/api/refund/**",
            "/api/review/**",
            "/api/coupon/**",
            "/api/upload/**"
    };

    /** 公开路径：店面浏览、注册、登录 */
    private static final String[] PUBLIC_PATTERNS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/storefront/**"
    };

    private final MallAuthInterceptor mallAuthInterceptor;

    private final LocalFileStorage localFileStorage;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration registration = registry.addInterceptor(mallAuthInterceptor)
                .addPathPatterns(PROTECTED_PATTERNS)
                .excludePathPatterns(PUBLIC_PATTERNS);
        registration.order(10);
    }

    /**
     * 把上传目录映射成静态资源：{@code /files/xxx.jpg} → {@code <upload.dir>/xxx.jpg}。
     * 目录是运行时可配置的，所以用绝对路径的 file: 形式，而不是 classpath。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pattern = localFileStorage.publicPrefix() + "/**";
        String location = localFileStorage.root().toUri().toString();
        registry.addResourceHandler(pattern)
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
