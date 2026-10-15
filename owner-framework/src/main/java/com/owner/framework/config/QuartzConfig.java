package com.owner.framework.config;

import com.owner.framework.quartz.PortalHeartbeatJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Quartz 定时任务配置。
 * <p>任务存储使用 JDBC JobStore（{@code spring.quartz.job-store-type=jdbc}），
 * 表结构见 {@code sql/init.sql} 中的 QRTZ_* 表。</p>
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

    /**
     * 示例任务：门户心跳统计，默认每分钟执行一次。
     * 关闭方式：{@code owner.quartz.heartbeat.enabled=false}
     */
    @Bean
    @ConditionalOnProperty(prefix = "owner.quartz.heartbeat", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public JobDetail portalHeartbeatJobDetail() {
        return JobBuilder.newJob(PortalHeartbeatJob.class)
                .withIdentity("portalHeartbeatJob", "owner")
                .withDescription("门户心跳统计任务")
                .storeDurably()
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "owner.quartz.heartbeat", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public Trigger portalHeartbeatTrigger(JobDetail portalHeartbeatJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("portalHeartbeatTrigger", "owner")
                .forJob(portalHeartbeatJobDetail)
                .withDescription("门户心跳统计触发器：每分钟一次")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?")
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
