package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.vo.StatsSummaryVO;
import com.owner.system.kanban.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板统计接口（PRD §2 模块4 数据统计看板的后端支撑）。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "看板-统计", description = "状态分布 / 逾期 / 趋势（ECharts 数据源）")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "统计汇总", description = "状态饼图 + 逾期数 + 近 N 天新增/完成趋势，days 默认 14")
    @GetMapping("/summary")
    public R<StatsSummaryVO> summary(
            @Parameter(description = "项目ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "趋势天数（1~90，默认 14）") @RequestParam(required = false) Integer days) {
        return R.ok(statsService.summary(projectId, days));
    }
}
