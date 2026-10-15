package com.owner.system.kanban.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** AI 任务拆解入参。 */
@Data
public class AiSplitTaskDTO {

    /** 目标所属项目（用于上下文，可不传） */
    private Long projectId;

    @NotBlank(message = "目标描述不能为空")
    @Size(max = 1000, message = "目标描述最长 1000 字")
    private String goal;

    /** 期望拆解出的子任务数量上限，默认 8 */
    private Integer maxCount;
}
