package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.dto.AiOptimizeTaskDTO;
import com.owner.system.kanban.domain.dto.AiReportDTO;
import com.owner.system.kanban.domain.dto.AiSplitTaskDTO;
import com.owner.system.kanban.domain.vo.AiSubTaskVO;
import com.owner.system.kanban.service.KanbanAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 看板 AI 接口（PRD §6 AI接口，全部手动触发）。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "看板-AI", description = "AI 任务拆解 / 项目周报 / 任务描述润色（需配置 OPENAI_API_KEY）")
public class KanbanAiController {

    private final KanbanAiService kanbanAiService;

    @Operation(summary = "AI 拆解目标生成任务列表", description = "返回子任务建议，前端确认后调 POST /api/task/batch 落库")
    @PostMapping("/split-task")
    public R<List<AiSubTaskVO>> splitTask(@Valid @RequestBody AiSplitTaskDTO dto) {
        return R.ok(kanbanAiService.splitTask(dto));
    }

    @Operation(summary = "AI 生成项目周报", description = "按时间范围读取任务数据，生成 Markdown 周报文本")
    @PostMapping("/generate-report")
    public R<String> generateReport(@Valid @RequestBody AiReportDTO dto) {
        return R.ok(kanbanAiService.generateReport(dto));
    }

    @Operation(summary = "AI 润色任务描述", description = "返回润色后文本（不落库，用户确认后自行保存）")
    @PostMapping("/optimize-task")
    public R<String> optimizeTask(@Valid @RequestBody AiOptimizeTaskDTO dto) {
        return R.ok(kanbanAiService.optimizeTask(dto));
    }
}
