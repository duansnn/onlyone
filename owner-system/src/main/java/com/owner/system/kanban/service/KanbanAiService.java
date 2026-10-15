package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.ai.AiChatService;
import com.owner.system.kanban.domain.dto.AiOptimizeTaskDTO;
import com.owner.system.kanban.domain.dto.AiReportDTO;
import com.owner.system.kanban.domain.dto.AiSplitTaskDTO;
import com.owner.system.kanban.domain.entity.Project;
import com.owner.system.kanban.domain.entity.Task;
import com.owner.system.kanban.domain.vo.AiSubTaskVO;
import com.owner.system.kanban.domain.vo.TaskVO;
import com.owner.system.kanban.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 看板 AI 能力：任务拆解 / 项目周报 / 任务描述润色。
 * <p>全部手动触发；模型与密钥由 spring.ai.openai.* 配置
 * （通义千问 / DeepSeek 均为 OpenAI 兼容端点，切换只改环境变量）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanAiService {

    private static final Set<String> PRIORITIES = Set.of("high", "medium", "low");

    private final AiChatService aiChatService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /** AI 任务拆解：目标 -> 子任务建议列表（前端确认后调批量创建落库）。 */
    public List<AiSubTaskVO> splitTask(AiSplitTaskDTO dto) {
        checkAiReady();
        if (dto.getProjectId() != null) {
            projectService.getOwnedProject(dto.getProjectId());
        }
        int maxCount = dto.getMaxCount() == null || dto.getMaxCount() < 1 || dto.getMaxCount() > 20
                ? 8 : dto.getMaxCount();

        String system = """
                你是资深项目管理助手。把用户的目标拆解为可直接执行的任务清单。
                只输出一个 JSON 数组，不要输出任何解释文字或代码块标记。
                数组每个元素格式：{"title":"任务标题","description":"任务描述","priority":"high|medium|low","tags":"标签1,标签2"}
                """;
        String user = "目标：" + dto.getGoal() + "\n请拆解出不超过 " + maxCount
                + " 条子任务，按建议的执行顺序排列，标题控制在 30 字内。";

        String raw = callAi(system, user);
        List<AiSubTaskVO> items = parseArray(raw, maxCount);
        if (items.isEmpty()) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "AI 返回格式异常，请重试");
        }
        log.info("[Kanban][AI] 任务拆解：{} -> {} 条子任务", dto.getGoal(), items.size());
        return items;
    }

    /** AI 生成项目周报：读取时间范围内任务，产出 Markdown 周报文本。 */
    public String generateReport(AiReportDTO dto) {
        checkAiReady();
        Project project = projectService.getOwnedProject(dto.getProjectId());
        LocalDate end = dto.getEndDate() == null ? LocalDate.now() : dto.getEndDate();
        LocalDate start = dto.getStartDate() == null ? end.minusDays(6) : dto.getStartDate();
        if (start.isAfter(end)) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }

        List<Task> tasks = taskMapper.selectList(
                Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, project.getId()));

        long newCount = tasks.stream().map(Task::getCreateTime)
                .filter(t -> t != null && between(t.toLocalDate(), start, end)).count();
        long doneCount = tasks.stream()
                .filter(t -> "done".equals(t.getStatus()))
                .filter(t -> t.getUpdateTime() != null && between(t.getUpdateTime().toLocalDate(), start, end))
                .count();
        List<String> overdueList = tasks.stream().filter(TaskVO::isOverdue)
                .map(t -> "- " + t.getTitle() + "（截止 " + t.getDueDate() + "）").toList();
        List<String> doingList = tasks.stream()
                .filter(t -> "doing".equals(t.getStatus()))
                .map(t -> "- " + t.getTitle() + "（截止 " + t.getDueDate() + "，优先级 " + t.getPriority() + "）").toList();

        StringBuilder facts = new StringBuilder();
        facts.append("项目：").append(project.getName()).append('\n')
                .append("时间范围：").append(start).append(" ~ ").append(end).append('\n')
                .append("任务总数：").append(tasks.size())
                .append("，待办：").append(tasks.stream().filter(t -> "todo".equals(t.getStatus())).count())
                .append("，进行中：").append(doingList.size())
                .append("，已完成：").append(tasks.stream().filter(t -> "done".equals(t.getStatus())).count())
                .append("，逾期：").append(overdueList.size()).append('\n')
                .append("期内新增：").append(newCount).append(" 条，期内完成：").append(doneCount).append(" 条\n");
        if (!overdueList.isEmpty()) {
            facts.append("逾期任务：\n").append(String.join("\n", overdueList)).append('\n');
        }
        if (!doingList.isEmpty()) {
            facts.append("进行中任务：\n").append(String.join("\n", doingList)).append('\n');
        }

        String system = """
                你是项目经理，根据任务数据撰写简洁的中文项目周报（Markdown 格式）。
                章节包含：一、总体进展；二、关键成果；三、风险与逾期事项；四、下周建议。
                直接输出周报正文，不要额外解释。全文 300 字以内。
                """;
        String report = callAi(system, "以下是项目任务数据统计，请生成周报：\n" + facts);
        log.info("[Kanban][AI] 生成周报：projectId={}, {} ~ {}", project.getId(), start, end);
        return report;
    }

    /** AI 润色任务描述：返回润色后的文本（不落库，由用户确认后再保存）。 */
    public String optimizeTask(AiOptimizeTaskDTO dto) {
        checkAiReady();
        Task task = taskService.getOwnedTask(dto.getTaskId());
        String system = """
                你是文案专家，帮用户润色任务描述，使其更清晰、具体、可执行。
                只输出润色后的描述文本，不要解释、不要加引号。保持中文，100 字以内。
                """;
        String original = task.getDescription() == null || task.getDescription().isBlank()
                ? "（原描述为空，请根据标题补写一段简短的可执行描述）"
                : task.getDescription();
        String result = callAi(system, "任务标题：" + task.getTitle() + "\n原描述：" + original);
        log.info("[Kanban][AI] 润色任务描述：taskId={}", dto.getTaskId());
        return result == null ? "" : result.trim();
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** AI 是否已正确配置（Bean 存在且密钥不是占位值）。 */
    private void checkAiReady() {
        if (!aiChatService.available() || apiKey == null || apiKey.isBlank()
                || "sk-not-configured".equals(apiKey)) {
            throw new ServiceException("AI 能力未启用：请配置环境变量 OPENAI_API_KEY"
                    + "（通义千问 base-url=https://dashscope.aliyuncs.com/compatible-mode，"
                    + "DeepSeek base-url=https://api.deepseek.com）");
        }
    }

    private String callAi(String system, String user) {
        try {
            return aiChatService.chat(system, user);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Kanban][AI] 调用失败：{}", e.getMessage());
            throw new ServiceException("AI 调用失败，请稍后重试或检查模型配置");
        }
    }

    /** 从 AI 输出中提取 JSON 数组并清洗为子任务建议。 */
    private List<AiSubTaskVO> parseArray(String raw, int maxCount) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        // 去掉可能的代码块围栏，截取第一个 [ 到最后一个 ]
        String body = raw.replace("```json", "").replace("```", "").trim();
        int l = body.indexOf('[');
        int r = body.lastIndexOf(']');
        if (l < 0 || r <= l) {
            return List.of();
        }
        List<AiSubTaskVO> parsed = JsonUtils.parseList(body.substring(l, r + 1), AiSubTaskVO.class);
        List<AiSubTaskVO> result = new ArrayList<>();
        for (AiSubTaskVO item : parsed) {
            if (item == null || item.getTitle() == null || item.getTitle().isBlank()) {
                continue;
            }
            item.setTitle(item.getTitle().trim());
            if (item.getPriority() == null || !PRIORITIES.contains(item.getPriority())) {
                item.setPriority("medium");
            }
            result.add(item);
            if (result.size() >= maxCount) {
                break;
            }
        }
        return result;
    }

    private boolean between(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
