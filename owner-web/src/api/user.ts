import { del, get, post, put } from '@/api/request';
import type {
  KanbanUserStat,
  PageResult,
  SysUser,
  SysUserCreatePayload,
  SysUserQuery,
  SysUserUpdatePayload,
} from '@/types/api';

/** 分页查询门户用户 */
export function pageUsers(query: SysUserQuery): Promise<PageResult<SysUser>> {
  return get<PageResult<SysUser>>('/portal/users', query);
}

/** 按 ID 查询（后端走 Redis 缓存） */
export function getUserById(id: string): Promise<SysUser> {
  return get<SysUser>(`/portal/users/${id}`);
}

/** 新增用户，返回雪花 ID（字符串形式，避免精度丢失） */
export function createUser(payload: SysUserCreatePayload): Promise<string> {
  return post<string>('/portal/users', payload);
}

/** 修改用户（基于 @Version 乐观锁） */
export function updateUser(payload: SysUserUpdatePayload): Promise<void> {
  return put<void>('/portal/users', payload);
}

/** 逻辑删除用户 */
export function removeUser(id: string): Promise<void> {
  return del<void>(`/portal/users/${id}`);
}

/** 向所有 WebSocket 客户端推送该用户的欢迎消息 */
export function pushWelcome(id: string): Promise<void> {
  return post<void>(`/portal/users/${id}/welcome`);
}

/** 看板注册用户列表（只读，附项目/任务数） */
export function listKanbanUsers(): Promise<KanbanUserStat[]> {
  return get<KanbanUserStat[]>('/portal/users/kanban');
}

/** 新增看板用户 */
export function createKanbanUser(username: string, password: string): Promise<string> {
  return post<string>('/portal/users/kanban', { username, password });
}

/** 重置看板用户密码 */
export function resetKanbanPassword(id: string, password: string): Promise<void> {
  return put<void>(`/portal/users/kanban/${id}/password`, { password });
}

/** 删除看板用户（级联删除项目/任务/评论） */
export function removeKanbanUser(id: string): Promise<void> {
  return del<void>(`/portal/users/kanban/${id}`);
}
