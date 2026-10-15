package com.owner.system.kanban.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库笔记（个人笔记，复用看板登录态，按 userId 隔离）。
 */
@Data
@TableName("note")
public class Note implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 id */
    private Long userId;

    /** 标题 */
    private String title;

    /** 正文（纯文本/简版 Markdown） */
    private String content;

    /** 分类，如 学习/工作/生活 */
    private String category;

    /** 是否置顶 */
    private Integer pinned;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
