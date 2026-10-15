package com.owner.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增门户用户入参。
 */
@Data
@Schema(description = "新增门户用户入参")
public class SysUserCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号", example = "zhangsan")
    @NotBlank(message = "登录账号不能为空")
    @Size(min = 3, max = 32, message = "登录账号长度需在 3~32 之间")
    private String username;

    @Schema(description = "昵称", example = "张三")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称长度不能超过 32")
    private String nickname;

    @Schema(description = "手机号", example = "13800000000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @Schema(description = "邮箱", example = "zhangsan@owner.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "所属部门", example = "研发中心")
    private String deptName;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
