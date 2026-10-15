package com.owner.framework.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 门户心跳统计任务（示例），用于验证 Quartz JDBC JobStore 正常工作。
 * <p>使用 {@link DisallowConcurrentExecution} 避免集群环境下重复执行。</p>
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class PortalHeartbeatJob implements Job {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final AtomicLong EXECUTION_COUNT = new AtomicLong();

    @Value("${spring.application.name:owner-server}")
    private String applicationName;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long count = EXECUTION_COUNT.incrementAndGet();
        String fireTime = FORMATTER.format(LocalDateTime.now());
        log.info("[Quartz] {} 心跳任务第 {} 次执行，fireTime={}, jobKey={}",
                applicationName, count, fireTime, context.getJobDetail().getKey());
    }

    /** 已执行次数，供健康检查接口展示任务确实在跑 */
    public static long executionCount() {
        return EXECUTION_COUNT.get();
    }
}
