package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.system.kanban.domain.entity.Task;
import com.owner.system.kanban.domain.vo.StatsSummaryVO;
import com.owner.system.kanban.domain.vo.TaskVO;
import com.owner.system.kanban.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计看板服务：状态分布（饼图）、逾期统计、近 N 天新增/完成趋势（折线图）。
 * <p>数据量在看板量级（单项目几百条）内，直接内存聚合，简单且一次查询搞定。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final TaskMapper taskMapper;
    private final ProjectService projectService;

    /** 汇总统计，days 为趋势天数（默认 14，含今天）。 */
    public StatsSummaryVO summary(Long projectId, Integer days) {
        projectService.getOwnedProject(projectId);
        int span = (days == null || days < 1 || days > 90) ? 14 : days;
        List<Task> tasks = taskMapper.selectList(
                Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, projectId));

        long todo = 0, doing = 0, done = 0, overdue = 0;
        for (Task task : tasks) {
            switch (task.getStatus() == null ? "" : task.getStatus()) {
                case "todo" -> todo++;
                case "doing" -> doing++;
                case "done" -> done++;
                default -> { }
            }
            if (TaskVO.isOverdue(task)) {
                overdue++;
            }
        }

        // 近 N 天趋势（含今天）
        LocalDate today = LocalDate.now();
        List<StatsSummaryVO.TrendPoint> trend = new ArrayList<>(span);
        for (int i = span - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
            long newCount = tasks.stream()
                    .filter(t -> inRange(t.getCreateTime(), dayStart, dayEnd))
                    .count();
            long doneCount = tasks.stream()
                    .filter(t -> "done".equals(t.getStatus()))
                    .filter(t -> inRange(t.getUpdateTime(), dayStart, dayEnd))
                    .count();
            trend.add(new StatsSummaryVO.TrendPoint(day.toString(), newCount, doneCount));
        }
        return new StatsSummaryVO((long) tasks.size(), todo, doing, done, overdue, trend);
    }

    private boolean inRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        return time != null && !time.isBefore(start) && time.isBefore(end);
    }
}
