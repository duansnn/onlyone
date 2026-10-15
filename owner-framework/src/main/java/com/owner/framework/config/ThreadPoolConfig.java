package com.owner.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置：业务异步线程池 + 定时任务调度线程池。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class ThreadPoolConfig {

    /** 线程池核心线程数 */
    @Value("${owner.thread-pool.core-size:8}")
    private int coreSize;

    /** 线程池最大线程数 */
    @Value("${owner.thread-pool.max-size:32}")
    private int maxSize;

    /** 队列容量 */
    @Value("${owner.thread-pool.queue-capacity:1000}")
    private int queueCapacity;

    /** 空闲线程存活时间（秒） */
    @Value("${owner.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean("applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("owner-async-");
        // 队列满时由调用方线程执行，避免任务丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("[ThreadPool] 业务线程池初始化完成 core={}, max={}, queue={}", coreSize, maxSize, queueCapacity);
        return executor;
    }

    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(coreSize, 8));
        scheduler.setThreadNamePrefix("owner-scheduling-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
