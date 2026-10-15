package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.JsonUtils;
import com.owner.system.kanban.domain.dto.TaskImportItem;
import com.owner.system.kanban.domain.dto.TaskSaveDTO;
import com.owner.system.kanban.domain.entity.Project;
import com.owner.system.kanban.domain.entity.Task;
import com.owner.system.kanban.domain.vo.TaskExportVO;
import com.owner.system.kanban.domain.vo.TaskVO;
import com.owner.system.kanban.mapper.TaskMapper;
import com.owner.system.kanban.support.KanbanCurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板任务服务：CRUD、筛选搜索、拖拽改状态、批量创建、JSON 导入导出。
 * <p>任务的归属校验统一通过其所属项目完成（项目属于当前用户才可操作）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final ProjectService projectService;

    /**
     * 项目下任务列表，支持筛选与搜索。
     *
     * @param priority  优先级 high/medium/low
     * @param status    状态 todo/doing/done
     * @param tag       标签（模糊匹配）
     * @param keyword   关键词（匹配标题或描述）
     * @param dueBefore 截止日期早于等于该日（yyyy-MM-dd）
     * @param overdue   true 时仅返回逾期任务
     */
    public List<TaskVO> list(Long projectId, String priority, String status,
                             String tag, String keyword, LocalDate dueBefore, Boolean overdue) {
        projectService.getOwnedProject(projectId);
        List<Task> tasks = taskMapper.selectList(
                Wrappers.<Task>lambdaQuery()
                        .eq(Task::getProjectId, projectId)
                        .eq(priority != null, Task::getPriority, priority)
                        .eq(status != null, Task::getStatus, status)
                        .like(tag != null && !tag.isBlank(), Task::getTags, tag)
                        .le(dueBefore != null, Task::getDueDate, dueBefore)
                        .and(keyword != null && !keyword.isBlank(),
                                w -> w.like(Task::getTitle, keyword).or().like(Task::getDescription, keyword))
                        .orderByAsc(Task::getCreateTime));
        if (Boolean.TRUE.equals(overdue)) {
            tasks = tasks.stream().filter(TaskVO::isOverdue).toList();
        }
        return tasks.stream().map(TaskVO::of).toList();
    }

    /** 新增任务：title 必填，status 缺省 todo，priority 缺省 medium。 */
    public Long create(Long projectId, TaskSaveDTO dto) {
        projectService.getOwnedProject(projectId);
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "任务标题不能为空");
        }
        Task task = fromItem(projectId, dto.getTitle(), dto);
        taskMapper.insert(task);
        log.info("[Kanban] 新增任务：{} (id={}, projectId={})", task.getTitle(), task.getId(), projectId);
        return task.getId();
    }

    /** 更新任务（仅覆盖非空字段；拖拽改状态 = 只传 status）。 */
    public void update(Long taskId, TaskSaveDTO dto) {
        Task task = getOwnedTask(taskId);
        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new ServiceException(ResultCode.BAD_REQUEST, "任务标题不能为空");
            }
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        if (dto.getTags() != null) {
            task.setTags(dto.getTags());
        }
        taskMapper.updateById(task);
    }

    /** 删除任务。 */
    public void delete(Long taskId) {
        Task task = getOwnedTask(taskId);
        taskMapper.deleteById(task.getId());
        log.info("[Kanban] 删除任务：{} (id={})", task.getTitle(), taskId);
    }

    /** 批量创建（AI 拆解结果一键落库 / 导入复用），全部进入指定项目。 */
    @Transactional(rollbackFor = Exception.class)
    public Integer batchCreate(Long projectId, List<TaskImportItem> items) {
        projectService.getOwnedProject(projectId);
        if (items == null || items.isEmpty()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "导入内容为空");
        }
        int count = 0;
        for (TaskImportItem item : items) {
            if (item.getTitle() == null || item.getTitle().isBlank()) {
                continue; // 跳过无标题脏数据
            }
            taskMapper.insert(fromItem(projectId, item.getTitle(), item));
            count++;
        }
        log.info("[Kanban] 批量创建 {} 条任务 (projectId={})", count, projectId);
        return count;
    }

    /** 导出当前项目全部任务为 JSON 备份结构。 */
    public TaskExportVO export(Long projectId) {
        Project project = projectService.getOwnedProject(projectId);
        List<TaskVO> tasks = taskMapper.selectList(
                        Wrappers.<Task>lambdaQuery()
                                .eq(Task::getProjectId, projectId)
                                .orderByAsc(Task::getCreateTime))
                .stream().map(TaskVO::of).toList();
        TaskExportVO vo = new TaskExportVO();
        vo.setProjectId(project.getId());
        vo.setProjectName(project.getName());
        vo.setExportedAt(java.time.LocalDateTime.now());
        vo.setTaskCount(tasks.size());
        vo.setTasks(tasks);
        return vo;
    }

    /**
     * 导入 JSON 文件到当前项目。
     * <p>兼容两种格式：导出结构 {@code {"tasks":[...]}} 或裸数组 {@code [...]}</p>
     *
     * @return 实际导入条数
     */
    public Integer importTasks(Long projectId, MultipartFile file) {
        projectService.getOwnedProject(projectId);
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "上传文件为空");
        }
        List<TaskImportItem> items;
        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            JsonNode root = JsonUtils.readTree(json);
            if (root == null) {
                throw new IllegalArgumentException("JSON 解析失败");
            }
            JsonNode array = root.isArray() ? root : root.path("tasks");
            if (!array.isArray()) {
                throw new IllegalArgumentException("JSON 应为任务数组或含 tasks 数组");
            }
            items = new ArrayList<>(array.size());
            for (JsonNode node : array) {
                TaskImportItem item = JsonUtils.convert(node, TaskImportItem.class);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "文件内容不是合法的 JSON：" + e.getMessage());
        }
        return batchCreate(projectId, items);
    }

    /** 取当前用户名下的任务（经由所属项目校验归属）。 */
    public Task getOwnedTask(Long taskId) {
        if (taskId == null) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "taskId 不能为空");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "任务不存在：" + taskId);
        }
        // 归属校验：任务所属项目必须是当前用户的
        projectService.getOwnedProject(task.getProjectId());
        return task;
    }

    /** 由入参构造任务实体（缺省值：status=todo，priority=medium）。 */
    private Task fromItem(Long projectId, String title, TaskImportItem item) {
        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(title.trim());
        task.setDescription(item.getDescription());
        task.setDueDate(item.getDueDate());
        task.setPriority(item.getPriority() == null ? "medium" : item.getPriority());
        task.setStatus(item.getStatus() == null ? "todo" : item.getStatus());
        task.setTags(item.getTags());
        return task;
    }
}
