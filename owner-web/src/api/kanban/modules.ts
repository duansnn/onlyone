import { kanbanGet, kanbanPost, kanbanPut, kanbanDelete } from './request';
import type { Article, FocusSession, FocusToday, KanbanNote } from '@/types/kanban';

/** 知识库笔记 */
export const kanbanNoteApi = {
  list(keyword?: string, category?: string): Promise<KanbanNote[]> {
    return kanbanGet<KanbanNote[]>('/note', { keyword, category });
  },
  create(payload: { title: string; content: string; category?: string }): Promise<string> {
    return kanbanPost<string>('/note', payload);
  },
  update(id: string, payload: Partial<KanbanNote>): Promise<void> {
    return kanbanPut<void>(`/note/${id}`, payload);
  },
  remove(id: string): Promise<void> {
    return kanbanDelete<void>(`/note/${id}`);
  },
};

/** 番茄专注 */
export const kanbanFocusApi = {
  add(payload: { minutes: number; taskId?: string | null; note?: string }): Promise<string> {
    return kanbanPost<string>('/focus', payload);
  },
  today(): Promise<FocusToday> {
    return kanbanGet<FocusToday>('/focus/today');
  },
  trend(): Promise<{ date: string; minutes: number }[]> {
    return kanbanGet<{ date: string; minutes: number }[]>('/focus/trend');
  },
};

/** 博客文章 */
export const kanbanArticleApi = {
  list(params?: { status?: string; category?: string; keyword?: string }): Promise<Article[]> {
    return kanbanGet<Article[]>('/article', params);
  },
  save(payload: Partial<Article>): Promise<string> {
    return kanbanPost<string>('/article', payload);
  },
  detail(id: string): Promise<Article> {
    return kanbanGet<Article>(`/article/${id}`);
  },
  remove(id: string): Promise<void> {
    return kanbanDelete<void>(`/article/${id}`);
  },
};
