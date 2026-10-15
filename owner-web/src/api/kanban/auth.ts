import { kanbanPost } from './request';
import type {
  KanbanLoginVO,
  LoginPayload,
  RegisterPayload,
} from '@/types/kanban';

/** 看板账号接口：POST /api/user/register、/api/user/login */
export const kanbanAuthApi = {
  /** 用户注册，返回新用户 id */
  register(payload: RegisterPayload): Promise<number> {
    return kanbanPost<number>('/user/register', payload);
  },
  /** 用户登录，返回 JWT + 用户信息 */
  login(payload: LoginPayload): Promise<KanbanLoginVO> {
    return kanbanPost<KanbanLoginVO>('/user/login', payload);
  },
};
