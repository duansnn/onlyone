import { kanbanGet, kanbanPost, kanbanPut, kanbanDelete } from './request';
import type {
  KanbanComment,
  KanbanTask,
  TaskExport,
  TaskListQuery,
  TaskSavePayload,
  TaskImportItem,
} from '@/types/kanban';

/** 看板任务接口：/api/task（含导入导出） */
export const kanbanTaskApi = {
  /** 获取项目下全部任务（支持筛选/搜索/逾期） */
  list(query: TaskListQuery): Promise<KanbanTask[]> {
    return kanbanGet<KanbanTask[]>('/task/list', query);
  },
  /** 新增任务，返回新任务 id（字符串） */
  create(projectId: string, payload: TaskSavePayload): Promise<string> {
    return kanbanPost<string>('/task', payload, { projectId });
  },
  /** 批量创建任务（AI 拆解结果一键落库） */
  batchCreate(projectId: string, items: TaskImportItem[]): Promise<number> {
    return kanbanPost<number>('/task/batch', items, { projectId });
  },
  /** 更新任务（拖拽改状态也只传 status） */
  update(id: string, payload: TaskSavePayload): Promise<void> {
    return kanbanPut<void>(`/task/${id}`, payload);
  },
  /** 删除任务 */
  remove(id: string): Promise<void> {
    return kanbanDelete<void>(`/task/${id}`);
  },
  /** 导出项目任务 JSON */
  export(projectId: string): Promise<TaskExport> {
    return kanbanGet<TaskExport>('/task/export', { projectId });
  },
  /** 导入 JSON 批量创建任务（multipart 上传） */
  import(projectId: string, file: File): Promise<number> {
    const formData = new FormData();
    formData.append('file', file);
    return kanbanPost<number>('/task/import', formData, { projectId });
  },
  /** 任务评论列表 */
  listComments(taskId: string): Promise<KanbanComment[]> {
    return kanbanGet<KanbanComment[]>(`/task/${taskId}/comment`);
  },
  /** 发表评论 */
  addComment(taskId: string, content: string): Promise<string> {
    return kanbanPost<string>(`/task/${taskId}/comment`, { content });
  },
  /** 删除评论 */
  removeComment(commentId: string): Promise<void> {
    return kanbanDelete<void>(`/task/comment/${commentId}`);
  },
};
