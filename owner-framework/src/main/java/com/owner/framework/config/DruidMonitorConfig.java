package com.owner.framework.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Druid 监控页面配置。
 * <p>由于数据源由 dynamic-datasource 接管，这里排除了 Druid 自带的
 * {@code DruidDataSourceAutoConfigure}，改为手动注册监控 Servlet 与 Filter。</p>
 * <ul>
 *     <li>监控地址：http://localhost:41452/druid/</li>
 *     <li>关闭方式：{@code owner.druid.monitor.enabled=false}</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DruidDataSource.class)
@ConditionalOnProperty(prefix = "owner.druid.monitor", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class DruidMonitorConfig {

    public static final String MONITOR_PATH = "/druid/*";

    @Bean
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet(
            @Value("${owner.druid.monitor.login-username:admin}") String username,
            @Value("${owner.druid.monitor.login-password:change-me}") String password,
            @Value("${owner.druid.monitor.allow:}") String allow) {
        ServletRegistrationBean<StatViewServlet> registration =
                new ServletRegistrationBean<>(new StatViewServlet(), MONITOR_PATH);
        registration.addInitParameter("loginUsername", username);
        registration.addInitParameter("loginPassword", password);
        registration.addInitParameter("resetEnable", "false");
        if (allow != null && !allow.isBlank()) {
            registration.addInitParameter("allow", allow);
        }
        registration.setName("druidStatViewServlet");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> registration =
                new FilterRegistrationBean<>(new WebStatFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("exclusions",
                "*.js,*.gif,*.jpg,*.jpeg,*.png,*.css,*.ico,/druid/*,/doc.html,/webjars/*,/v3/api-docs/*");
        registration.addInitParameter("profileEnable", "true");
        registration.setName("druidWebStatFilter");
        registration.setOrder(2);
        return registration;
    }
}
