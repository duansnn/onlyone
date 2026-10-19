import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import type { R } from '@/types/api';
import { toast } from '@/utils/feedback';
import { useAuthStore } from '@/store/auth';

/** 业务异常：携带后端返回的 code / msg */
export class BizError extends Error {
  code: number;
  constructor(code: number, msg: string) {
    super(msg);
    this.name = 'BizError';
    this.code = code;
  }
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
});

let traceSeq = 0;

service.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`);
    }
    // 与后端 owner.framework.web.TraceIdInterceptor 约定：优先读 X-Trace-Id
    config.headers.set('X-Trace-Id', `${Date.now().toString(36)}-${(traceSeq++).toString(36)}`);
    return config;
  },
  (error) => Promise.reject(error),
);

service.interceptors.response.use(
  (response) => {
    const body = response.data as R;

    // 非统一响应结构（如 actuator / 静态资源）直接返回原始 data
    if (!body || typeof body.code !== 'number') {
      return response.data;
    }

    if (body.code === 200) {
      // 解包 data，调用侧直接拿到业务数据
      return body.data;
    }

    const msg = body.msg || `请求失败（${body.code}）`;
    toast.error(msg);
    return Promise.reject(new BizError(body.code, msg));
  },
  (error: AxiosError) => {
    if (error.code === 'ERR_NETWORK') {
      toast.error('无法连接后端服务，请确认 owner-server 已在 41452 端口启动');
    } else if (error.response) {
      const status = error.response.status;
      const body = error.response.data as R | undefined;
      toast.error(body?.msg || `请求异常：HTTP ${status}`);
    } else if (error.code === 'ECONNABORTED') {
      toast.error('请求超时，请稍后重试');
    } else {
      toast.error(error.message || '请求失败');
    }
    return Promise.reject(error);
  },
);

export function get<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as unknown as Promise<T>;
}

export function post<T>(url: string, data?: unknown, params?: object): Promise<T> {
  return service.post(url, data, { params }) as unknown as Promise<T>;
}

export function put<T>(url: string, data?: unknown, params?: object): Promise<T> {
  return service.put(url, data, { params }) as unknown as Promise<T>;
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config) as unknown as Promise<T>;
}

/**
 * 上传单个文件。
 *
 * 默认头是 application/json，这里必须让浏览器自己写 boundary；
 * axios 检测到 FormData 时会清掉 Content-Type 并由浏览器补全。
 */
export function uploadFile<T>(url: string, file: File, params?: object): Promise<T> {
  const form = new FormData();
  form.append('file', file);
  return service.post(url, form, {
    params,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  }) as unknown as Promise<T>;
}

export default service;
