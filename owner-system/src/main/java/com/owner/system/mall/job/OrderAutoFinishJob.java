package com.owner.system.mall.job;

import com.owner.system.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * 自动确认收货：每天扫描待收货满 7 天的订单，改成已完成。
 */
@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class OrderAutoFinishJob implements Job {

    private final OrderService orderService;

    @Override
    public void execute(JobExecutionContext context) {
        int finished = orderService.autoFinishOrders();
        if (finished > 0) {
            log.info("[OrderJob] 自动确认收货 {} 笔订单", finished);
        }
    }
}
