package com.owner.system.kanban.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务看板用户（PRD 表 user，独立于门户 sys_user）。
 * <p>主键自增，密码为 BCrypt 密文，登录态由 JWT 维持。</p>
 */
@Data
@TableName("`user`")
public class KanbanUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 id（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    /** 创建时间（数据库默认值） */
    private LocalDateTime createTime;

    /** 更新时间（数据库 ON UPDATE） */
    private LocalDateTime updateTime;
}
