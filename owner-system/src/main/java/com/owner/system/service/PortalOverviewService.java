package com.owner.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.domain.vo.PortalOverviewVO;
import com.owner.system.kanban.domain.entity.Task;
import com.owner.system.kanban.mapper.TaskMapper;
import com.owner.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户仪表盘总览服务：聚合门户与看板核心指标（只读）。
 */
@Service
@RequiredArgsConstructor
public class PortalOverviewService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SysUserMapper sysUserMapper;
    private final com.owner.system.kanban.mapper.KanbanUserMapper kanbanUserMapper;
    private final com.owner.system.kanban.mapper.ProjectMapper projectMapper;
    private final TaskMapper taskMapper;

    public PortalOverviewVO overview(int trendDays) {
        PortalOverviewVO vo = new PortalOverviewVO();
        vo.setPortalUserCount(sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDelFlag, 0)));
        vo.setKanbanUserCount(kanbanUserMapper.selectCount(null));

        vo.setProjectCount(projectMapper.selectCount(null));
        long totalTasks = 0;
        long todo = 0;
        long doing = 0;
        long done = 0;
        long overdue = 0;

        List<Task> tasks = taskMapper.selectList(Wrappers.emptyWrapper());
        totalTasks = tasks.size();
        LocalDate today = LocalDate.now();
        for (Task t : tasks) {
            switch (t.getStatus() == null ? "" : t.getStatus()) {
                case "todo" -> todo++;
                case "doing" -> doing++;
                case "done" -> done++;
                default -> { }
            }
            boolean notDone = !"done".equals(t.getStatus());
            if (notDone && t.getDueDate() != null && t.getDueDate().isBefore(today)) {
                overdue++;
            }
        }
        vo.setTaskCount(totalTasks);
        vo.setTodoCount(todo);
        vo.setDoingCount(doing);
        vo.setDoneCount(done);
        vo.setOverdueCount(overdue);
        vo.setTrend(buildTrend(tasks, trendDays));
        return vo;
    }

    /** 近 N 天新增/完成趋势：新增按 create_time，完成按 update_time 且 status=done。 */
    private List<PortalOverviewVO.TrendPoint> buildTrend(List<Task> tasks, int days) {
        LocalDate today = LocalDate.now();
        Map<String, long[]> byDate = new HashMap<>();
        for (Task t : tasks) {
            if (t.getCreateTime() != null) {
                String d = t.getCreateTime().toLocalDate().format(DATE);
                byDate.computeIfAbsent(d, k -> new long[2])[0]++;
            }
            if ("done".equals(t.getStatus()) && t.getUpdateTime() != null) {
                String d = t.getUpdateTime().toLocalDate().format(DATE);
                byDate.computeIfAbsent(d, k -> new long[2])[1]++;
            }
        }
        List<PortalOverviewVO.TrendPoint> trend = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String d = today.minusDays(i).format(DATE);
            long[] counts = byDate.getOrDefault(d, new long[2]);
            PortalOverviewVO.TrendPoint p = new PortalOverviewVO.TrendPoint();
            p.setDate(d);
            p.setNewCount(counts[0]);
            p.setDoneCount(counts[1]);
            trend.add(p);
        }
        return trend;
    }
}
