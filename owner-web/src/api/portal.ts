import { get, post } from '@/api/request';
import type { PortalInfo } from '@/types/api';

/** 应用信息与运行状态 */
export function fetchPortalInfo(): Promise<PortalInfo> {
  return get<PortalInfo>('/portal/info');
}

/** 广播 WebSocket 消息（按 owner.websocket.send-mode 配置的方式发送） */
export function broadcastMessage(content: string): Promise<{ sendMode: string; content: string }> {
  return post<{ sendMode: string; content: string }>('/portal/websocket/broadcast', undefined, { content });
}

/** 定向推送 WebSocket 消息 */
export function sendMessageToClient(clientId: string, content: string): Promise<void> {
  return post<void>('/portal/websocket/send', undefined, { clientId, content });
}

/** AI 对话（Spring AI 2.0.0） */
export function aiChat(prompt: string): Promise<string> {
  return get<string>('/portal/ai/chat', { prompt });
}
