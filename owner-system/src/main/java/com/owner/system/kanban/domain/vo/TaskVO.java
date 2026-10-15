package com.owner.system.kanban.domain.vo;

import com.owner.system.kanban.domain.entity.Task;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 任务卡片视图。 */
@Data
public class TaskVO {

    private Long id;

    private Long projectId;

    private String title;

    private String description;

    private LocalDate dueDate;

    /** high / medium / low */
    private String priority;

    /** todo / doing / done */
    private String status;

    /** 标签，逗号分隔 */
    private String tags;

    /** 是否逾期：截止日期早于今天且未完成 */
    private Boolean overdue;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static TaskVO of(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setProjectId(task.getProjectId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setDueDate(task.getDueDate());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setTags(task.getTags());
        vo.setOverdue(isOverdue(task));
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    public static boolean isOverdue(Task task) {
        return task.getDueDate() != null
                && !"done".equals(task.getStatus())
                && task.getDueDate().isBefore(LocalDate.now());
    }
}
