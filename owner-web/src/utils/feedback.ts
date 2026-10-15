import { create } from 'zustand';

/** antd 消息实例的最小接口，避免直接依赖 antd 内部类型 */
export interface ToastApi {
  success: (content: string) => void;
  error: (content: string) => void;
  warning: (content: string) => void;
  info: (content: string) => void;
  loading: (content: string) => void;
}

interface ToastState {
  api: ToastApi | null;
  setApi: (api: ToastApi | null) => void;
}

/**
 * antd v6 推荐通过 App.useApp() 获取 message 实例（可获得主题/上下文）。
 * 这里用一个极轻的桥接 store，让非组件模块（如 axios 拦截器）也能弹出提示。
 */
export const useToastStore = create<ToastState>((set) => ({
  api: null,
  setApi: (api) => set({ api }),
}));

/** 统一提示入口 */
export const toast: ToastApi = {
  success: (content) => useToastStore.getState().api?.success(content),
  error: (content) => useToastStore.getState().api?.error(content),
  warning: (content) => useToastStore.getState().api?.warning(content),
  info: (content) => useToastStore.getState().api?.info(content),
  loading: (content) => useToastStore.getState().api?.loading(content),
};
