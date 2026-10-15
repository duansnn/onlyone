package com.owner.system.kanban.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 番茄专注记录（复用看板登录态，按 userId 隔离）。
 */
@Data
@TableName("focus_session")
public class FocusSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 id */
    private Long userId;

    /** 关联任务 id（可空） */
    private Long taskId;

    /** 专注时长（分钟） */
    private Integer minutes;

    /** 备注 */
    private String note;

    private LocalDateTime createTime;
}
