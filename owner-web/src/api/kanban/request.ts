import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import type { R } from '@/types/api';
import { toast } from '@/utils/feedback';
import { useKanbanAuthStore } from '@/store/kanbanAuth';

/** 业务异常：携带后端返回的 code / msg */
export class KanbanBizError extends Error {
  code: number;
  constructor(code: number, msg: string) {
    super(msg);
    this.name = 'KanbanBizError';
    this.code = code;
  }
}

/**
 * 看板专用 axios 实例：token 取自看板独立登录态，与门户 request 互不干扰。
 * 看板后端所有接口（除 register/login）都要求 Authorization: Bearer <jwt>。
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

service.interceptors.request.use((config) => {
  const token = useKanbanAuthStore.getState().token;
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

/** 供拦截器内部重定向到看板登录页，避免直接 import router 造成循环依赖 */
function redirectToKanbanLogin() {
  if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/kanban/login')) {
    // 清除看板登录态并跳转
    useKanbanAuthStore.getState().signOut();
    window.location.href = '/kanban/login';
  }
}

service.interceptors.response.use(
  (response) => {
    const body = response.data as R;

    if (!body || typeof body.code !== 'number') {
      return response.data;
    }

    if (body.code === 200) {
      return body.data;
    }

    // 401 未登录/令牌失效：清除登录态并跳转登录页
    if (body.code === 401) {
      toast.error(body.msg || '登录已过期，请重新登录');
      redirectToKanbanLogin();
      return Promise.reject(new KanbanBizError(body.code, body.msg || '未授权'));
    }

    const msg = body.msg || `请求失败（${body.code}）`;
    toast.error(msg);
    return Promise.reject(new KanbanBizError(body.code, msg));
  },
  (error: AxiosError) => {
    if (error.code === 'ERR_NETWORK') {
      toast.error('无法连接后端服务，请确认 owner-server 已在 41452 端口启动');
    } else if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        toast.error('登录已过期，请重新登录');
        redirectToKanbanLogin();
      } else {
        const body = error.response.data as R | undefined;
        toast.error(body?.msg || `请求异常：HTTP ${status}`);
      }
    } else if (error.code === 'ECONNABORTED') {
      toast.error('请求超时，请稍后重试');
    } else {
      toast.error(error.message || '请求失败');
    }
    return Promise.reject(error);
  },
);

export function kanbanGet<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as unknown as Promise<T>;
}

export function kanbanPost<T>(url: string, data?: unknown, params?: object): Promise<T> {
  return service.post(url, data, { params }) as unknown as Promise<T>;
}

export function kanbanPut<T>(url: string, data?: unknown): Promise<T> {
  return service.put(url, data) as unknown as Promise<T>;
}

export function kanbanDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config) as unknown as Promise<T>;
}

export default service;
