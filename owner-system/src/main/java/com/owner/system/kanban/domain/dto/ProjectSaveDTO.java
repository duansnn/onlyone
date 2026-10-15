package com.owner.system.kanban.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 项目创建/编辑入参（更新时仅覆盖非空字段）。 */
@Data
public class ProjectSaveDTO {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称最长 100 字")
    private String name;

    @Size(max = 2000, message = "项目描述最长 2000 字")
    private String description;
}
