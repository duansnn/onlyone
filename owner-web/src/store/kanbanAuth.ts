import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * 看板登录态（独立于门户的 sys_user 登录）。
 * 看板走 /api/user/login 的 JWT 体系，token 单独存储，避免与门户 auth 混淆。
 */
interface KanbanAuthState {
  token: string | null;
  userId: string | null;
  username: string | null;
  expireAt: string | null;
  signIn: (token: string, userId: string, username: string, expireAt: string) => void;
  signOut: () => void;
  isAuthenticated: () => boolean;
}

export const useKanbanAuthStore = create<KanbanAuthState>()(
  persist(
    (set, get) => ({
      token: null,
      userId: null,
      username: null,
      expireAt: null,
      signIn: (token, userId, username, expireAt) =>
        set({ token, userId, username, expireAt }),
      signOut: () => set({ token: null, userId: null, username: null, expireAt: null }),
      isAuthenticated: () => Boolean(get().token),
    }),
    { name: 'owner-kanban-auth' },
  ),
);
