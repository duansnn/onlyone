import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { SysUser } from '@/types/api';

interface AuthState {
  token: string | null;
  /** 雪花 ID，按字符串保存避免精度丢失 */
  userId: string | null;
  username: string | null;
  nickname: string | null;
  loginAt: number | null;
  /** 登录成功后写入；payload 为后端返回的门户用户 */
  signIn: (user: SysUser, token: string) => void;
  signOut: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      userId: null,
      username: null,
      nickname: null,
      loginAt: null,
      signIn: (user, token) =>
        set({
          token,
          userId: user.id,
          username: user.username,
          nickname: user.nickname,
          loginAt: Date.now(),
        }),
      signOut: () =>
        set({ token: null, userId: null, username: null, nickname: null, loginAt: null }),
      isAuthenticated: () => Boolean(get().token),
    }),
    { name: 'owner-portal-auth' },
  ),
);
