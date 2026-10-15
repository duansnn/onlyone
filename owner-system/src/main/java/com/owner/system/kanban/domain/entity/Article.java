package com.owner.system.kanban.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客文章（复用看板登录态，按 userId 隔离）。
 */
@Data
@TableName("article")
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作者用户 id */
    private Long userId;

    /** 标题 */
    private String title;

    /** 正文（Markdown） */
    private String content;

    /** 分类 */
    private String category;

    /** 标签，逗号分隔 */
    private String tags;

    /** 状态：draft 草稿 / published 已发布 */
    private String status;

    /** 摘要（列表展示用，截取自正文） */
    private String summary;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
