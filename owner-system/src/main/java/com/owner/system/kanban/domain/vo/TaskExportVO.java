package com.owner.system.kanban.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 任务导出 JSON 结构（导入时兼容同格式或裸数组）。 */
@Data
public class TaskExportVO {

    private Long projectId;

    private String projectName;

    /** 导出时间 */
    private LocalDateTime exportedAt;

    /** 任务条数 */
    private Integer taskCount;

    /** 任务明细 */
    private List<TaskVO> tasks;
}
