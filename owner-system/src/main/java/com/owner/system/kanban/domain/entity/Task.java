package com.owner.system.kanban.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务看板任务卡片（PRD 表 task）。
 */
@Data
@TableName("task")
public class Task implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务 id（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 id */
    private Long projectId;

    /** 任务标题 */
    private String title;

    /** 任务详情 */
    private String description;

    /** 截止日期 */
    private LocalDate dueDate;

    /** 优先级：high / medium / low */
    private String priority;

    /** 状态：todo / doing / done */
    private String status;

    /** 标签，多个逗号分隔 */
    private String tags;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 优先级合法值 */
    public static final String[] PRIORITIES = {"high", "medium", "low"};
    /** 状态合法值 */
    public static final String[] STATUSES = {"todo", "doing", "done"};
}
