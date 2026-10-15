package com.owner.system.kanban.service;

import com.owner.system.kanban.domain.entity.Project;
import com.owner.system.kanban.domain.entity.Task;
import com.owner.system.kanban.mapper.ProjectMapper;
import com.owner.system.kanban.mapper.TaskMapper;
import com.owner.system.kanban.support.KanbanCurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 演示种子数据服务：为当前用户一键预置演示项目与任务（PRD §8 面试演示方案）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanSeedService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;

    /** 为当前用户创建演示数据，返回创建的项目数。已有项目时不再重复灌入。 */
    @Transactional(rollbackFor = Exception.class)
    public long seed() {
        Long userId = KanbanCurrentUser.getRequiredUserId();
        Long existing = projectMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Project>lambdaQuery()
                        .eq(Project::getUserId, userId));
        if (existing != null && existing > 0) {
            throw new com.owner.common.core.exception.ServiceException(
                    com.owner.common.core.enums.ResultCode.BUSINESS_ERROR,
                    "你已有项目，为避免混入演示数据未执行。请先清空项目后再一键加载。");
        }

        LocalDate today = LocalDate.now();
        long projectCount = 0;

        // 演示项目 1：作品集网站开发
        Project p1 = createProject(userId, "作品集网站开发",
                "个人作品集站点，用于求职展示。技术栈 React 19 + Spring Boot 4。");
        batchInsert(p1.getId(), List.of(
                task("确定站点定位与页面结构", "梳理首页/项目页/关于我三大板块的信息架构。", today.minusDays(12), "high", "done", "规划", 13),
                task("搭建前端工程（Vite+TS）", "初始化 Vite 工程，配置 ESLint 与路径别名。", today.minusDays(10), "medium", "done", "前端", 11),
                task("设计数据库表结构", "user/project/task 三表 + 索引设计，输出 init.sql。", today.minusDays(9), "high", "done", "后端", 9),
                task("开发看板拖拽交互", "HTML5 原生拖拽，三列卡片实时保存状态。", today.minusDays(3), "high", "doing", "前端,核心", 2),
                task("对接通义千问 API", "配置 API Key，联调 AI 任务拆解/周报/润色。", today.plusDays(2), "medium", "doing", "AI", 1),
                task("编写 README 与部署文档", "含本地启动说明与线上访问地址。", today.minusDays(1), "low", "todo", "文档", 0),
                task("移动端响应式适配", "窄屏下三列改为纵向堆叠。", today.plusDays(5), "low", "todo", "前端", 0),
                task("性能优化：首屏加载 <2s", "路由懒加载 + ECharts 按需引入。", today.plusDays(7), "medium", "todo", "优化", 0)));
        projectCount++;

        // 演示项目 2：求职准备
        Project p2 = createProject(userId, "求职准备",
                "2026 春招：简历、算法、八股、面试复盘。");
        batchInsert(p2.getId(), List.of(
                task("更新简历（项目经历）", "突出看板项目的技术亮点：JWT 自研、乐观锁、雪花 ID 精度处理。", today.minusDays(6), "high", "doing", "简历", 4),
                task("刷完 hot 100 链表篇", "LeetCode 热题 100 中链表与二叉树部分。", today.plusDays(10), "medium", "doing", "算法", 3),
                task("复盘上次模拟面试", "整理答得不好的问题：JVM 调优、Redis 持久化。", today.minusDays(2), "medium", "done", "面试", 2),
                task("准备自我介绍 1 分钟版", "针对 Java 后端岗，突出全栈与工程化。", today.minusDays(4), "high", "todo", "面试", 0),
                task("整理 Spring Boot 4 新特性笔记", "Jackson 3、虚拟线程、模块化变化。", today.plusDays(3), "low", "todo", "八股", 0),
                task("投递 10 家目标公司", "Boss 直聘 + 内推渠道各一半。", today.minusDays(1), "high", "todo", "投递", 0)));
        projectCount++;

        log.info("[Kanban] 用户 {} 一键加载演示数据完成，共 {} 个项目", userId, projectCount);
        return projectCount;
    }

    private Project createProject(Long userId, String name, String description) {
        Project p = new Project();
        p.setUserId(userId);
        p.setName(name);
        p.setDescription(description);
        projectMapper.insert(p);
        return p;
    }

    private void batchInsert(Long projectId, List<Task> tasks) {
        for (Task t : tasks) {
            t.setProjectId(projectId);
            taskMapper.insert(t);
        }
    }

    /** 构造一条任务。dueDate 已由调用方按 today±N 天算好（早于今天即逾期）；dayOffset 仅作可读性占位。 */
    @SuppressWarnings("unused")
    private Task task(String title, String description, LocalDate dueDate,
                      String priority, String status, String tags, int dayOffset) {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription(description);
        t.setDueDate(dueDate);
        t.setPriority(priority);
        t.setStatus(status);
        t.setTags(tags);
        return t;
    }
}
