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

/**
 * 是否店主（能进店铺后台）。
 * <p>优先看后端下发的 role；旧版本 localStorage 里没有 role 字段时退回 platformAdmin，
 * 避免升级后已登录的账号突然进不了后台。</p>
 */
export function isMerchant(user: AuthUser | null | undefined): boolean {
  if (!user) return false;
  if (user.role) return user.role === 'MERCHANT';
  return Boolean(user.platformAdmin);
}

/** 是否是该店铺的店主（OWNER 才能改店铺资料/店员） */
export function isShopOwner(shops: ShopBrief[], shopId: string | null): boolean {
  if (!shopId) return false;
  return shops.some((s) => s.shopId === shopId && s.role === 'OWNER');
}

/** 当前操作店铺里的身份（没传 shopId 或找不到时退回第一家店） */
export function shopRoleOf(shops: ShopBrief[], shopId: string | null): ShopBrief | undefined {
  return shops.find((s) => String(s.shopId) === String(shopId)) ?? shops[0];
}

/**
 * 店铺内的身份文案：店主 / 店员。
 * <p>{@code user.role} 是「能不能进后台」的粗粒度身份，店主和店员都是 MERCHANT，
 * 界面上要区分是哪一种，必须看 shop_member 里的 role，不能直接拿 roleLabel 当店主。</p>
 */
export function shopRoleLabel(shops: ShopBrief[], shopId: string | null): string | null {
  const role = shopRoleOf(shops, shopId)?.role;
  if (role === 'OWNER') return '店主';
  if (role === 'STAFF') return '店员';
  return null;
}
