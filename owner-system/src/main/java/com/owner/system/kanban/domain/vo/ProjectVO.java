package com.owner.system.kanban.domain.vo;

import com.owner.system.kanban.domain.entity.Project;
import lombok.Data;

import java.time.LocalDateTime;

/** 项目列表项（附任务数，供首页卡片展示）。 */
@Data
public class ProjectVO {

    private Long id;

    private String name;

    private String description;

    /** 项目下任务总数（基本类型 long，避免被 Long->String 序列化器转成字符串） */
    private long taskCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static ProjectVO of(Project project, Long taskCount) {
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setDescription(project.getDescription());
        vo.setTaskCount(taskCount == null ? 0L : taskCount);
        vo.setCreateTime(project.getCreateTime());
        vo.setUpdateTime(project.getUpdateTime());
        return vo;
    }
}
