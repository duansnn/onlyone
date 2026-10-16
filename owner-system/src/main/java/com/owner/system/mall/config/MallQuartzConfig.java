package com.owner.system.mall.config;

import com.owner.system.mall.job.OrderAutoFinishJob;
import com.owner.system.mall.job.OrderTimeoutCancelJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 电商订单的定时任务。
 *
 * <ul>
 *   <li>待付款超时取消：每分钟一次，{@code owner.mall.order.timeout-job.enabled}</li>
 *   <li>发货满 7 天自动确认收货：每天 02:10，{@code owner.mall.order.auto-finish-job.enabled}</li>
 * </ul>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class MallQuartzConfig {

    @Bean
    @ConditionalOnProperty(prefix = "owner.mall.order.timeout-job", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public JobDetail orderTimeoutCancelJobDetail() {
        return JobBuilder.newJob(OrderTimeoutCancelJob.class)
                .withIdentity("orderTimeoutCancelJob", "mall")
                .withDescription("待付款超时取消（每分钟）")
                .storeDurably()
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "owner.mall.order.timeout-job", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public Trigger orderTimeoutCancelTrigger(JobDetail orderTimeoutCancelJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("orderTimeoutCancelTrigger", "mall")
                .forJob(orderTimeoutCancelJobDetail)
                .withDescription("每分钟扫描待付款超过 30 分钟的订单并释放库存")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?")
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "owner.mall.order.auto-finish-job", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public JobDetail orderAutoFinishJobDetail() {
        return JobBuilder.newJob(OrderAutoFinishJob.class)
                .withIdentity("orderAutoFinishJob", "mall")
                .withDescription("发货满 7 天自动确认收货（每天）")
                .storeDurably()
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "owner.mall.order.auto-finish-job", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public Trigger orderAutoFinishTrigger(JobDetail orderAutoFinishJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("orderAutoFinishTrigger", "mall")
                .forJob(orderAutoFinishJobDetail)
                .withDescription("每天 02:10 把发货满 7 天的待收货订单改成已完成")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 10 2 * * ?")
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
