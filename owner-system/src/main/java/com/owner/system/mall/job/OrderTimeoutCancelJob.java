package com.owner.system.mall.job;

import com.owner.system.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * 待付款超时取消：每分钟扫描待付款超过 30 分钟的订单，取消并释放锁定库存。
 */
@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class OrderTimeoutCancelJob implements Job {

    private final OrderService orderService;

    @Override
    public void execute(JobExecutionContext context) {
        int cancelled = orderService.cancelExpiredOrders();
        if (cancelled > 0) {
            log.info("[OrderJob] 超时取消 {} 笔待付款订单", cancelled);
        }
    }
}
