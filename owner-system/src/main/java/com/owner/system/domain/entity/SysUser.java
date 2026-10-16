package com.owner.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户。
 * <p>全站唯一账号体系：顾客、店主、店员、平台超管都是这一张表，
 * 具体身份由 {@code platform_admin} 与 {@code shop_member} 决定。</p>
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** BCrypt 密码散列 */
    private String passwordHash;

    /** 平台超级管理员：0 否，1 是（koko 为 1） */
    private Integer platformAdmin;

    /** 头像 */
    private String avatarUrl;

    /** 所属部门（电商权限不再使用，字段保留） */
    private String deptName;

    /** 状态：0 正常，1 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    /** 版本号（乐观锁） */
    @Version
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
