package com.owner.system.kanban.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** AI 生成项目周报入参。 */
@Data
public class AiReportDTO {

    @NotNull(message = "projectId 不能为空")
    private Long projectId;

    /** 统计开始日期（含），默认 7 天前 */
    private LocalDate startDate;

    /** 统计结束日期（含），默认今天 */
    private LocalDate endDate;
}
