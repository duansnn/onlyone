package com.owner.system.kanban.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** AI 任务描述润色入参。 */
@Data
public class AiOptimizeTaskDTO {

    @NotNull(message = "taskId 不能为空")
    private Long taskId;
}
