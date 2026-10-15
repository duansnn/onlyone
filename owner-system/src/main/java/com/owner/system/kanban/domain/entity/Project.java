package com.owner.system.kanban.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务看板项目（PRD 表 project），任务是项目的子资源。
 */
@Data
@TableName("project")
public class Project implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目 id（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 id */
    private Long userId;

    /** 项目名称 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
