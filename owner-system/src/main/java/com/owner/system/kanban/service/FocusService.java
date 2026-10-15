package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.system.kanban.domain.entity.FocusSession;
import com.owner.system.kanban.mapper.FocusSessionMapper;
import com.owner.system.kanban.support.KanbanCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 番茄专注记录服务（数据按 userId 隔离）。
 */
@Service
@RequiredArgsConstructor
public class FocusService {

    private final FocusSessionMapper focusSessionMapper;

    /** 记录一次专注。 */
    public Long add(Integer minutes, Long taskId, String note) {
        FocusSession s = new FocusSession();
        s.setUserId(KanbanCurrentUser.getRequiredUserId());
        s.setMinutes(minutes == null ? 25 : minutes);
        s.setTaskId(taskId);
        s.setNote(note);
        focusSessionMapper.insert(s);
        return s.getId();
    }

    /** 今日统计：专注次数、总分钟数。 */
    public Map<String, Object> todayStats() {
        Long userId = KanbanCurrentUser.getRequiredUserId();
        LocalDate today = LocalDate.now();
        List<FocusSession> list = focusSessionMapper.selectList(
                Wrappers.<FocusSession>lambdaQuery().eq(FocusSession::getUserId, userId));
        int count = 0;
        int minutes = 0;
        for (FocusSession s : list) {
            if (s.getCreateTime() != null && s.getCreateTime().toLocalDate().equals(today)) {
                count++;
                minutes += s.getMinutes() == null ? 0 : s.getMinutes();
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("todayCount", count);
        data.put("todayMinutes", minutes);
        data.put("totalCount", list.size());
        return data;
    }

    /** 近 7 天每日专注分钟数（前端画柱状图）。 */
    public List<Map<String, Object>> last7Days() {
        Long userId = KanbanCurrentUser.getRequiredUserId();
        List<FocusSession> list = focusSessionMapper.selectList(
                Wrappers.<FocusSession>lambdaQuery().eq(FocusSession::getUserId, userId));
        LocalDate today = LocalDate.now();
        Map<String, Integer> byDate = new HashMap<>();
        for (FocusSession s : list) {
            if (s.getCreateTime() == null) continue;
            String d = s.getCreateTime().toLocalDate().toString();
            byDate.merge(d, s.getMinutes() == null ? 0 : s.getMinutes(), Integer::sum);
        }
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> {
                    String d = today.minusDays(6 - i).toString();
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", d);
                    row.put("minutes", byDate.getOrDefault(d, 0));
                    return row;
                })
                .toList();
    }
}
