import { kanbanGet, kanbanPost, kanbanPut, kanbanDelete } from './request';
import type { KanbanProject, ProjectSavePayload } from '@/types/kanban';

/** 看板项目接口：/api/project */
export const kanbanProjectApi = {
  /** 获取当前用户全部项目（附任务数） */
  list(): Promise<KanbanProject[]> {
    return kanbanGet<KanbanProject[]>('/project/list');
  },
  /** 创建新项目，返回新项目 id（字符串） */
  create(payload: ProjectSavePayload): Promise<string> {
    return kanbanPost<string>('/project', payload);
  },
  /** 修改项目名称/描述 */
  update(id: string, payload: ProjectSavePayload): Promise<void> {
    return kanbanPut<void>(`/project/${id}`, payload);
  },
  /** 删除项目（级联删除任务） */
  remove(id: string): Promise<void> {
    return kanbanDelete<void>(`/project/${id}`);
  },
  /** 一键加载演示数据（已有项目时后端会拒绝） */
  seed(): Promise<number> {
    return kanbanPost<number>('/project/seed');
  },
};
