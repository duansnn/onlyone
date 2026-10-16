import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthUser, LoginResult, ShopBrief } from '@/types/mall';

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  /** 当前账号在各店铺里的身份（决定是否能看到店铺后台） */
  shops: ShopBrief[];
  /** 店铺后台当前操作的店铺 */
  currentShopId: string | null;
  signedInAt: number | null;
  signIn: (result: LoginResult, user: AuthUser) => void;
  setUser: (user: AuthUser) => void;
  setShops: (shops: ShopBrief[]) => void;
  setCurrentShop: (shopId: string) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      shops: [],
      currentShopId: null,
      signedInAt: null,
      signIn: (result, user) =>
        set({
          token: result.token,
          user,
          shops: result.shops ?? [],
          currentShopId: result.shops?.[0]?.shopId ?? null,
          signedInAt: Date.now(),
        }),
      setUser: (user) => set({ user }),
      setShops: (shops) =>
        set({ shops, currentShopId: get().currentShopId ?? shops[0]?.shopId ?? null }),
      setCurrentShop: (shopId) => set({ currentShopId: shopId }),
      signOut: () =>
        set({ token: null, user: null, shops: [], currentShopId: null, signedInAt: null }),
    }),
    { name: 'owner-mall-auth' },
  ),
);

/** 是否拥有任一店铺身份 */
export function hasShopRole(shops: ShopBrief[]): boolean {
  return shops.length > 0;
}

/** 是否是该店铺的店主（OWNER 才能改店铺资料/店员） */
export function isShopOwner(shops: ShopBrief[], shopId: string | null): boolean {
  if (!shopId) return false;
  return shops.some((s) => s.shopId === shopId && s.role === 'OWNER');
}
