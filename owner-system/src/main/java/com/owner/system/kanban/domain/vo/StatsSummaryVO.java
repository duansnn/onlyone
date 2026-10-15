package com.owner.system.kanban.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 统计看板汇总数据：状态分布 + 逾期 + 趋势。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsSummaryVO {

    /** 任务总数 */
    private long total;

    /** 待办数量 */
    private long todoCount;

    /** 进行中数量 */
    private long doingCount;

    /** 已完成数量 */
    private long doneCount;

    /** 逾期数量（截止日期早于今天且未完成） */
    private long overdueCount;

    /** 近 N 天趋势：每日新增 / 完成数量 */
    private List<TrendPoint> trend;

    /** 趋势单点。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        /** 日期 yyyy-MM-dd */
        private String date;
        /** 当日新增任务数 */
        private long newCount;
        /** 当日完成任务数（以 update_time 近似完成时间） */
        private long doneCount;
    }
}
