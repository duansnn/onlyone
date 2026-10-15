package com.owner.system.domain.dto;

import com.owner.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 门户用户分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "门户用户分页查询条件")
public class SysUserQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号，模糊匹配", example = "zhang")
    private String username;

    @Schema(description = "昵称，模糊匹配", example = "张")
    private String nickname;

    @Schema(description = "手机号", example = "13800000000")
    private String mobile;

    @Schema(description = "所属部门", example = "研发中心")
    private String deptName;

    @Schema(description = "状态：0 正常，1 停用", example = "0")
    private Integer status;
}
