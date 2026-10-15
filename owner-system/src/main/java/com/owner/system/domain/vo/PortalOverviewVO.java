package com.owner.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 门户仪表盘总览出参：门户 + 看板核心指标与近 N 天趋势。
 */
@Data
@Schema(description = "门户仪表盘总览出参")
public class PortalOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "门户用户数（sys_user）")
    private long portalUserCount;

    @Schema(description = "看板注册用户数（user）")
    private long kanbanUserCount;

    @Schema(description = "看板项目总数")
    private long projectCount;

    @Schema(description = "看板任务总数")
    private long taskCount;

    @Schema(description = "待办任务数")
    private long todoCount;

    @Schema(description = "进行中任务数")
    private long doingCount;

    @Schema(description = "已完成任务数")
    private long doneCount;

    @Schema(description = "逾期任务数（未完成且截止日期早于今天）")
    private long overdueCount;

    @Schema(description = "近 N 天趋势")
    private List<TrendPoint> trend;

    @Data
    @Schema(description = "趋势点")
    public static class TrendPoint implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "日期 yyyy-MM-dd", example = "2026-10-15")
        private String date;

        @Schema(description = "当日新增任务数")
        private long newCount;

        @Schema(description = "当日完成任务数")
        private long doneCount;
    }
}
