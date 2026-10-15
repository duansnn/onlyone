package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.kanban.domain.dto.ProjectSaveDTO;
import com.owner.system.kanban.domain.entity.Project;
import com.owner.system.kanban.domain.entity.Task;
import com.owner.system.kanban.domain.vo.ProjectVO;
import com.owner.system.kanban.mapper.ProjectMapper;
import com.owner.system.kanban.mapper.TaskMapper;
import com.owner.system.kanban.support.KanbanCurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板项目服务：所有操作都限定当前登录用户自己的项目。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;

    /** 当前用户全部项目（附任务数）。 */
    public List<ProjectVO> list() {
        Long userId = KanbanCurrentUser.getRequiredUserId();
        List<Project> projects = projectMapper.selectList(
                Wrappers.<Project>lambdaQuery()
                        .eq(Project::getUserId, userId)
                        .orderByDesc(Project::getCreateTime));
        if (projects.isEmpty()) {
            return List.of();
        }
        // 一次 group by 查出各项目任务数，避免 N+1
        List<Long> ids = projects.stream().map(Project::getId).toList();
        Map<Long, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : taskMapper.selectMaps(
                Wrappers.<Task>query()
                        .select("project_id", "COUNT(*) AS cnt")
                        .in("project_id", ids)
                        .groupBy("project_id"))) {
            Object pid = row.get("project_id");
            Object cnt = row.get("cnt");
            if (pid instanceof Number n && cnt instanceof Number c) {
                countMap.put(n.longValue(), c.longValue());
            }
        }
        return projects.stream()
                .map(p -> ProjectVO.of(p, countMap.get(p.getId())))
                .toList();
    }

    /** 新建项目。 */
    public Long create(ProjectSaveDTO dto) {
        Project project = new Project();
        project.setUserId(KanbanCurrentUser.getRequiredUserId());
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        projectMapper.insert(project);
        log.info("[Kanban] 新建项目：{} (id={})", project.getName(), project.getId());
        return project.getId();
    }

    /** 编辑项目名称/描述（仅覆盖非空字段）。 */
    public void update(Long id, ProjectSaveDTO dto) {
        Project project = getOwnedProject(id);
        if (dto.getName() != null && !dto.getName().isBlank()) {
            project.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }
        projectMapper.updateById(project);
    }

    /** 删除项目并级联删除其下全部任务。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Project project = getOwnedProject(id);
        int removedTasks = taskMapper.delete(
                Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, project.getId()));
        projectMapper.deleteById(project.getId());
        log.info("[Kanban] 删除项目 {} (id={})，级联删除任务 {} 条", project.getName(), id, removedTasks);
    }

    /** 取当前用户名下的项目，不存在或非本人时抛业务异常。 */
    public Project getOwnedProject(Long projectId) {
        if (projectId == null) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "projectId 不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "项目不存在：" + projectId);
        }
        if (!project.getUserId().equals(KanbanCurrentUser.getRequiredUserId())) {
            throw new ServiceException(ResultCode.FORBIDDEN, "无权访问该项目");
        }
        return project;
    }
}
