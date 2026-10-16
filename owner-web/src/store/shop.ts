import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { storeApi } from '@/api/mall';
import type { StoreHome } from '@/types/mall';

interface ShopState {
  /** 店面当前浏览的店铺；未登录也要能逛，所以持久化到本地 */
  shopId: string | null;
  shopName: string | null;
  home: StoreHome | null;
  loading: boolean;
  setShop: (shopId: string, shopName?: string) => void;
  loadHome: (shopId?: string, force?: boolean) => Promise<void>;
}

export const useShopStore = create<ShopState>()(
  persist(
    (set, get) => ({
      shopId: null,
      shopName: null,
      home: null,
      loading: false,
      setShop: (shopId, shopName) => set({ shopId, shopName: shopName ?? get().shopName }),
      loadHome: async (shopId, force) => {
        const target = shopId ?? get().shopId;
        if (!target) return;
        if (!force && get().home?.shopId === target) return;
        set({ loading: true });
        try {
          const home = await storeApi.home(target);
          set({ home, shopId: home.shopId, shopName: home.shopName });
        } finally {
          set({ loading: false });
        }
      },
    }),
    { name: 'owner-mall-shop', partialize: (s) => ({ shopId: s.shopId, shopName: s.shopName }) },
  ),
);
