package com.owner.system.kanban.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 任务导入/AI 批量创建的单条任务项。
 * <p>对应导出 JSON 中 tasks 数组的元素，或 AI 拆解结果。</p>
 */
@Data
public class TaskImportItem {

    @Size(max = 200, message = "任务标题最长 200 字")
    private String title;

    @Size(max = 5000, message = "任务详情最长 5000 字")
    private String description;

    private LocalDate dueDate;

    @Pattern(regexp = "^(high|medium|low)$", message = "优先级只能是 high/medium/low")
    private String priority;

    @Pattern(regexp = "^(todo|doing|done)$", message = "状态只能是 todo/doing/done")
    private String status;

    @Size(max = 200, message = "标签最长 200 字")
    private String tags;
}
