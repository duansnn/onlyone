package com.owner.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Quartz 定时任务配置。
 * <p>任务存储使用 JDBC JobStore（{@code spring.quartz.job-store-type=jdbc}），
 * 表结构见 {@code sql/init.sql} 中的 QRTZ_* 表。</p>
 * <p>本类只负责把数据源交给 Quartz；具体业务任务由各业务模块自行注册，
 * 例如电商的 {@code MallQuartzConfig}（订单超时取消、自动完成）。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class QuartzConfig {

    /**
     * 把数据源显式交给 Quartz。
     * <p>本工程的数据源由 dynamic-datasource 接管（Bean 名为 {@code dataSource}），
     * Spring Boot 自带的 JDBC JobStore 装配条件在此组合下不会生效，
     * 因此这里显式绑定，否则会抛出 {@code DataSource name not set}。</p>
     */
    @Bean
    public SchedulerFactoryBeanCustomizer quartzDataSourceCustomizer(ApplicationContext applicationContext) {
        return factory -> {
            DataSource dataSource = null;
            if (applicationContext.containsBean("dataSource")) {
                dataSource = applicationContext.getBean("dataSource", DataSource.class);
            }
            if (dataSource == null) {
                dataSource = applicationContext.getBeanProvider(DataSource.class).getIfUnique();
            }
            if (dataSource != null) {
                factory.setDataSource(dataSource);
                log.info("[Quartz] JDBC JobStore 已绑定数据源: {}", dataSource.getClass().getSimpleName());
            } else {
                log.error("[Quartz] 未找到可用的数据源，JDBC JobStore 将无法工作");
            }
        };
    }
}
