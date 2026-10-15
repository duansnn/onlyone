/**
 * 看板模块类型定义，与后端 com.owner.system.kanban 下的 VO / DTO 一一对应。
 *
 * 关键：后端 owner-framework 的 JacksonConfig 注册了 Long -> String 序列化器，
 * 因此所有 Long 包装类型字段（id / projectId / userId / expireAt）都会被序列化为
 * 字符串；而基本类型 long（taskCount / total / todoCount / newCount 等）仍是数字。
 * 前端必须按 string 处理 id 类字段，避免精度丢失与类型不匹配。
 */

/** 后端统一把 Long 包装类型序列化为字符串 */
export type KanbanId = string;

/** 登录成功返回：LoginVO */
export interface KanbanLoginVO {
  token: string;
  userId: string;
  username: string;
  /** 令牌过期时间（epoch 毫秒，字符串） */
  expireAt: string;
}

/** 项目列表项：ProjectVO（taskCount 为基本类型 long，数字） */
export interface KanbanProject {
  id: string;
  name: string;
  description?: string;
  taskCount: number;
  createTime?: string;
  updateTime?: string;
}

/** 任务优先级 */
export type TaskPriority = 'high' | 'medium' | 'low';
/** 任务状态 */
export type TaskStatus = 'todo' | 'doing' | 'done';

/** 任务卡片：TaskVO */
export interface KanbanTask {
  id: string;
  projectId: string;
  title: string;
  description?: string;
  /** yyyy-MM-dd */
  dueDate?: string;
  priority: TaskPriority;
  status: TaskStatus;
  /** 标签，逗号分隔 */
  tags?: string;
  /** 是否逾期 */
  overdue?: boolean;
  createTime?: string;
  updateTime?: string;
}

/** 任务导出结构：TaskExportVO */
export interface TaskExport {
  projectId: string;
  projectName: string;
  exportedAt: string;
  taskCount: number;
  tasks: KanbanTask[];
}

/** 趋势单点：StatsSummaryVO.TrendPoint */
export interface TrendPoint {
  date: string;
  newCount: number;
  doneCount: number;
}

/** 统计汇总：StatsSummaryVO */
export interface StatsSummary {
  total: number;
  todoCount: number;
  doingCount: number;
  doneCount: number;
  overdueCount: number;
  trend: TrendPoint[];
}

/** AI 拆解出的子任务建议：AiSubTaskVO */
export interface AiSubTask {
  title: string;
  description?: string;
  priority?: TaskPriority;
  tags?: string;
}

/** 注册入参：RegisterDTO */
export interface RegisterPayload {
  username: string;
  password: string;
}

/** 登录入参：LoginDTO */
export interface LoginPayload {
  username: string;
  password: string;
}

/** 项目创建/编辑入参：ProjectSaveDTO */
export interface ProjectSavePayload {
  name: string;
  description?: string;
}

/** 任务新增/编辑入参：TaskSaveDTO（更新时仅覆盖非空字段） */
export interface TaskSavePayload {
  title?: string;
  description?: string;
  dueDate?: string;
  priority?: TaskPriority;
  status?: TaskStatus;
  tags?: string;
}

/** 任务导入/AI 批量创建的单条：TaskImportItem */
export interface TaskImportItem {
  title: string;
  description?: string;
  dueDate?: string;
  priority?: TaskPriority;
  status?: TaskStatus;
  tags?: string;
}

/** AI 拆解入参：AiSplitTaskDTO（projectId 可选，Long 类型） */
export interface AiSplitPayload {
  projectId?: string;
  goal: string;
  maxCount?: number;
}

/** AI 周报入参：AiReportDTO（projectId 必填，Long 类型） */
export interface AiReportPayload {
  projectId: string;
  startDate?: string;
  endDate?: string;
}

/** AI 润色入参：AiOptimizeTaskDTO（taskId 必填，Long 类型） */
export interface AiOptimizePayload {
  taskId: string;
}

/** 任务列表筛选参数 */
export interface TaskListQuery {
  projectId: string;
  priority?: TaskPriority;
  status?: TaskStatus;
  tag?: string;
  keyword?: string;
  /** 截止日期早于等于该日 yyyy-MM-dd */
  dueBefore?: string;
  overdue?: boolean;
}

/** 任务评论：TaskComment（后端 entity） */
export interface KanbanComment {
  id: string;
  taskId: string;
  author: string;
  content: string;
  createTime?: string;
}

/** 笔记：Note */
export interface KanbanNote {
  id: string;
  userId?: string;
  title: string;
  content: string;
  category?: string;
  pinned?: number;
  createTime?: string;
  updateTime?: string;
}

/** 专注记录：FocusSession */
export interface FocusSession {
  id: string;
  taskId?: string | null;
  minutes: number;
  note?: string;
  createTime?: string;
}

/** 今日专注统计 */
export interface FocusToday {
  todayCount: number;
  todayMinutes: number;
  totalCount: number;
}

/** 文章：Article */
export interface Article {
  id: string;
  title: string;
  content: string;
  category?: string;
  tags?: string;
  status: 'draft' | 'published';
  summary?: string;
  createTime?: string;
  updateTime?: string;
}
