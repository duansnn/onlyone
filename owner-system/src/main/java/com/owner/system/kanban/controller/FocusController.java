package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.service.FocusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 番茄专注接口。
 */
@RestController
@RequestMapping("/api/focus")
@RequiredArgsConstructor
@Tag(name = "番茄钟", description = "专注时长记录与统计")
public class FocusController {

    private final FocusService focusService;

    @Operation(summary = "记录一次专注")
    @PostMapping
    public R<Long> add(@RequestBody Map<String, Object> body) {
        Integer minutes = body.get("minutes") == null ? null : ((Number) body.get("minutes")).intValue();
        Long taskId = body.get("taskId") == null ? null : Long.valueOf(body.get("taskId").toString());
        return R.ok(focusService.add(minutes, taskId, body.get("note") == null ? null : body.get("note").toString()));
    }

    @Operation(summary = "今日统计")
    @GetMapping("/today")
    public R<Map<String, Object>> today() {
        return R.ok(focusService.todayStats());
    }

    @Operation(summary = "近 7 天专注趋势")
    @GetMapping("/trend")
    public R<List<Map<String, Object>>> trend() {
        return R.ok(focusService.last7Days());
    }
}
