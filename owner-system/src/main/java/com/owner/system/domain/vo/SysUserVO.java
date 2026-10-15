package com.owner.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门户用户出参。
 */
@Data
@Schema(description = "门户用户出参")
public class SysUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1001")
    private Long id;

    @Schema(description = "登录账号", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "脱敏后的手机号", example = "138****0000")
    private String mobile;

    @Schema(description = "邮箱", example = "zhangsan@owner.com")
    private String email;

    @Schema(description = "所属部门", example = "研发中心")
    private String deptName;

    @Schema(description = "状态：0 正常，1 停用", example = "0")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "版本号（乐观锁，修改时需原样回传）", example = "0")
    private Integer version;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
