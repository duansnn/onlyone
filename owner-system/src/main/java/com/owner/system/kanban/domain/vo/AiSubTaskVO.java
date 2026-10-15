package com.owner.system.kanban.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI 拆解出的子任务建议（前端确认后调 POST /api/task/batch 批量落库）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSubTaskVO {

    private String title;

    private String description;

    /** high / medium / low */
    private String priority;

    /** 标签，逗号分隔 */
    private String tags;
}
