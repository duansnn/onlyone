import { kanbanPost } from './request';
import type {
  AiSubTask,
  AiSplitPayload,
  AiReportPayload,
  AiOptimizePayload,
} from '@/types/kanban';

/** 看板 AI 接口：/api/ai（全部手动触发） */
export const kanbanAiApi = {
  /** AI 拆解目标生成子任务列表 */
  splitTask(payload: AiSplitPayload): Promise<AiSubTask[]> {
    return kanbanPost<AiSubTask[]>('/ai/split-task', payload);
  },
  /** AI 生成项目周报（Markdown 文本） */
  generateReport(payload: AiReportPayload): Promise<string> {
    return kanbanPost<string>('/ai/generate-report', payload);
  },
  /** AI 润色任务描述（返回润色后文本） */
  optimizeTask(payload: AiOptimizePayload): Promise<string> {
    return kanbanPost<string>('/ai/optimize-task', payload);
  },
};
