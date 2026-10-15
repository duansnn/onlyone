package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.system.kanban.domain.entity.KanbanUser;
import com.owner.system.kanban.domain.entity.Project;
import com.owner.system.kanban.domain.entity.Task;
import com.owner.system.kanban.domain.vo.KanbanUserStatVO;
import com.owner.system.kanban.mapper.KanbanUserMapper;
import com.owner.system.kanban.mapper.ProjectMapper;
import com.owner.system.kanban.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 看板用户统计服务：供门户只读展示看板注册用户及其项目/任务数量。
 * <p>不依赖看板登录态（KanbanCurrentUser），由门户侧直接调用。</p>
 */
@Service
@RequiredArgsConstructor
public class KanbanUserStatService {

    private final KanbanUserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final com.owner.system.kanban.mapper.TaskCommentMapper commentMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /** 管理员新增看板用户（用户名唯一，密码 BCrypt 落库）。 */
    public Long create(String username, String rawPassword) {
        Long exists = userMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<KanbanUser>lambdaQuery()
                        .eq(KanbanUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new com.owner.common.core.exception.ServiceException(
                    com.owner.common.core.enums.ResultCode.DATA_EXISTS, "用户名已被注册：" + username);
        }
        KanbanUser user = new KanbanUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.insert(user);
        return user.getId();
    }

    /** 管理员重置看板用户密码。 */
    public void resetPassword(Long userId, String rawPassword) {
        KanbanUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new com.owner.common.core.exception.ServiceException(
                    com.owner.common.core.enums.ResultCode.DATA_NOT_FOUND, "看板用户不存在：" + userId);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
    }

    /** 管理员删除看板用户：级联删除其项目、任务与评论。 */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        KanbanUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new com.owner.common.core.exception.ServiceException(
                    com.owner.common.core.enums.ResultCode.DATA_NOT_FOUND, "看板用户不存在：" + userId);
        }
        List<Project> projects = projectMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Project>lambdaQuery()
                        .eq(Project::getUserId, userId));
        if (!projects.isEmpty()) {
            List<Long> projectIds = projects.stream().map(Project::getId).toList();
            List<Task> tasks = taskMapper.selectList(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<Task>lambdaQuery()
                            .in(Task::getProjectId, projectIds));
            if (!tasks.isEmpty()) {
                List<Long> taskIds = tasks.stream().map(Task::getId).toList();
                commentMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<com.owner.system.kanban.domain.entity.TaskComment>lambdaQuery()
                        .in(com.owner.system.kanban.domain.entity.TaskComment::getTaskId, taskIds));
            }
            taskMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Task>lambdaQuery()
                    .in(Task::getProjectId, projectIds));
            projectMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Project>lambdaQuery()
                    .in(Project::getId, projectIds));
        }
        userMapper.deleteById(userId);
    }

    /** 返回全部看板注册用户，附项目数、任务数。 */
    public List<KanbanUserStatVO> listAll() {
        List<KanbanUser> users = userMapper.selectList(
                Wrappers.<KanbanUser>lambdaQuery().orderByDesc(KanbanUser::getCreateTime));

        // 每个用户的项目数
        Map<Long, Long> projectCount = new HashMap<>();
        // userId -> 项目 id 列表（用于统计任务数）
        Map<Long, List<Long>> userProjectIds = new HashMap<>();
        if (!users.isEmpty()) {
            List<Project> projects = projectMapper.selectList(
                    Wrappers.<Project>lambdaQuery()
                            .in(Project::getUserId, users.stream().map(KanbanUser::getId).toList()));
            for (Project p : projects) {
                Long uid = p.getUserId();
                projectCount.merge(uid, 1L, Long::sum);
                userProjectIds.computeIfAbsent(uid, k -> new java.util.ArrayList<>()).add(p.getId());
            }
        }

        // 每个用户的任务数（按项目批量 group by，避免 N+1）
        Map<Long, Long> taskCount = new HashMap<>();
        List<Long> allProjectIds = userProjectIds.values().stream()
                .flatMap(List::stream).toList();
        if (!allProjectIds.isEmpty()) {
            for (Map<String, Object> row : taskMapper.selectMaps(
                    Wrappers.<Task>query()
                            .select("project_id", "COUNT(*) AS cnt")
                            .in("project_id", allProjectIds)
                            .groupBy("project_id"))) {
                Object pid = row.get("project_id");
                Object cnt = row.get("cnt");
                if (pid instanceof Number pn && cnt instanceof Number cn) {
                    // 把项目级任务数归并到所属用户
                    Long uid = findOwner(userProjectIds, pn.longValue());
                    if (uid != null) {
                        taskCount.merge(uid, cn.longValue(), Long::sum);
                    }
                }
            }
        }

        return users.stream().map(u -> {
            KanbanUserStatVO vo = new KanbanUserStatVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setProjectCount(projectCount.getOrDefault(u.getId(), 0L));
            vo.setTaskCount(taskCount.getOrDefault(u.getId(), 0L));
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).toList();
    }

    /** 根据项目 id 反查所属用户 id。 */
    private Long findOwner(Map<Long, List<Long>> userProjectIds, long projectId) {
        for (Map.Entry<Long, List<Long>> e : userProjectIds.entrySet()) {
            if (e.getValue().stream().anyMatch(id -> Objects.equals(id, projectId))) {
                return e.getKey();
            }
        }
        return null;
    }
}
