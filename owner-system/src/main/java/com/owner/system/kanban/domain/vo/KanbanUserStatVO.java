package com.owner.system.kanban.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 看板用户统计出参（供门户「用户管理」页只读展示看板注册用户）。
 * 注意：password 等敏感字段不暴露。
 */
@Data
@Schema(description = "看板用户统计出参")
public class KanbanUserStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "看板用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "koko")
    private String username;

    @Schema(description = "项目数", example = "3")
    private long projectCount;

    @Schema(description = "任务数", example = "12")
    private long taskCount;

    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
