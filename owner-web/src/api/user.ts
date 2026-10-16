import { del, get, post, put } from '@/api/request';
import type {
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
