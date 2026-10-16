package com.owner.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 门户仪表盘总览出参。
 */
@Data
@Schema(description = "门户仪表盘总览出参")
public class PortalOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "门户用户数（sys_user）")
    private long portalUserCount;

    @Schema(description = "近 N 天趋势，业务清空后为空")
    private List<TrendPoint> trend;

    @Data
    @Schema(description = "趋势点")
    public static class TrendPoint implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "日期 yyyy-MM-dd", example = "2026-10-15")
        private String date;

        @Schema(description = "当日新增数")
        private long newCount;

        @Schema(description = "当日完成数")
        private long doneCount;
    }
}
