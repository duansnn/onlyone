package com.owner.system.kanban.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务评论（看板可选迭代功能）。
 */
@Data
@TableName("task_comment")
public class TaskComment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评论 id（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属任务 id */
    private Long taskId;

    /** 评论人（当前登录看板用户的用户名快照） */
    private String author;

    /** 评论内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;
}
