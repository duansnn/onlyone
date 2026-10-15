package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.dto.TaskImportItem;
import com.owner.system.kanban.domain.dto.TaskSaveDTO;
import com.owner.system.kanban.domain.vo.TaskExportVO;
import com.owner.system.kanban.domain.vo.TaskVO;
import com.owner.system.kanban.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 看板任务接口（PRD §6 任务 + 导入导出接口，另补批量创建供 AI 拆解落库）。
 */
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
@Tag(name = "看板-任务", description = "任务 CRUD / 筛选搜索 / 拖拽改状态 / JSON 导入导出")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "获取项目下全部任务", description = "支持按优先级/状态/标签/关键词/截止日期筛选，overdue=true 仅看逾期")
    @GetMapping("/list")
    public R<List<TaskVO>> list(
            @Parameter(description = "项目ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "优先级 high/medium/low") @RequestParam(required = false) String priority,
            @Parameter(description = "状态 todo/doing/done") @RequestParam(required = false) String status,
            @Parameter(description = "标签（模糊匹配）") @RequestParam(required = false) String tag,
            @Parameter(description = "关键词（匹配标题或描述）") @RequestParam(required = false) String keyword,
            @Parameter(description = "截止日期早于等于该日 yyyy-MM-dd") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @Parameter(description = "true 仅返回逾期任务") @RequestParam(required = false) Boolean overdue) {
        return R.ok(taskService.list(projectId, priority, status, tag, keyword, dueBefore, overdue));
    }

    @Operation(summary = "新增任务", description = "title 必填；status 缺省 todo，priority 缺省 medium")
    @PostMapping
    public R<Long> create(@Parameter(description = "项目ID", required = true) @RequestParam Long projectId,
                          @Valid @RequestBody TaskSaveDTO dto) {
        return R.ok(taskService.create(projectId, dto));
    }

    @Operation(summary = "批量创建任务", description = "AI 拆解结果一键落库（全部进入待办）")
    @PostMapping("/batch")
    public R<Integer> batchCreate(@Parameter(description = "项目ID", required = true) @RequestParam Long projectId,
                                  @Valid @RequestBody List<TaskImportItem> items) {
        return R.ok(taskService.batchCreate(projectId, items));
    }

    @Operation(summary = "更新任务", description = "仅覆盖非空字段；拖拽卡片 = 只传 status")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "任务ID") @PathVariable Long id,
                          @Valid @RequestBody TaskSaveDTO dto) {
        taskService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "任务ID") @PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    @Operation(summary = "导出项目任务 JSON", description = "返回备份结构，前端可直接保存为 .json 文件")
    @GetMapping("/export")
    public R<TaskExportVO> export(@Parameter(description = "项目ID", required = true) @RequestParam Long projectId) {
        return R.ok(taskService.export(projectId));
    }

    @Operation(summary = "导入 JSON 批量创建任务", description = "multipart 上传，兼容导出结构 {\"tasks\":[...]} 或裸数组 [...]")
    @PostMapping("/import")
    public R<Integer> importTasks(@Parameter(description = "项目ID", required = true) @RequestParam Long projectId,
                                  @Parameter(description = "JSON 文件") @RequestParam("file") MultipartFile file) {
        return R.ok(taskService.importTasks(projectId, file));
    }
}
