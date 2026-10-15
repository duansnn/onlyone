import { pageUsers } from '@/api/user';
import type { SysUser } from '@/types/api';

export interface LoginPayload {
  username: string;
  password: string;
}

/**
 * 门户登录。
 *
 * 说明：当前 owner-server 未提供独立的认证接口（无 /api/auth/login、无密码字段），
 * 因此这里复用「分页查询门户用户」接口完成账号校验：
 *   1. 按 username 精确匹配 sys_user；
 *   2. 校验账号存在且未被停用（status === 0）。
 * 密码在此阶段不做校验，仅用于占位。
 *
 * 后续接入真实认证时，只需把本函数替换为 POST /api/auth/login 即可，
 * 其余页面无需改动。
 */
export async function login(payload: LoginPayload): Promise<{ user: SysUser; token: string }> {
  const username = payload.username?.trim();
  if (!username) {
    throw new Error('请输入登录账号');
  }

  // username 在后端是 like 匹配，这里取回后做一次精确过滤
  const page = await pageUsers({ username, pageNum: 1, pageSize: 20 });
  const user = page.rows.find((item) => item.username === username);

  if (!user) {
    throw new Error('账号不存在，请检查后重试');
  }
  if (user.status === 1) {
    throw new Error('该账号已停用，请联系管理员');
  }

  // 前端会话令牌：仅用于本地登录态与请求头透传，非真实 JWT
  const token = `owner-portal-${btoa(unescape(encodeURIComponent(`${user.id}:${user.username}`)))}.${Date.now()}`;
  return { user, token };
}
