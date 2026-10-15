import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { WsMessage, WsStatus } from '@/types/api';

/** 解析握手地址：优先用 VITE_WS_BASE，否则按当前站点协议推导 */
export function resolveWsUrl(clientId: string): string {
  const base = import.meta.env.VITE_WS_BASE;
  const query = `?clientId=${encodeURIComponent(clientId)}`;
  if (base) {
    return `${base.replace(/\/+$/, '')}/ws/portal${query}`;
  }
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws/portal${query}`;
}

export interface UseWebSocketOptions {
  clientId: string;
  autoConnect?: boolean;
  /** 最多保留多少条历史消息 */
  maxMessages?: number;
}

export interface UseWebSocketResult {
  status: WsStatus;
  messages: WsMessage[];
  connected: boolean;
  url: string;
  connect: () => void;
  disconnect: () => void;
  send: (data: string) => boolean;
  clearMessages: () => void;
}

/**
 * 门户 WebSocket 客户端。
 *
 * 与后端 com.owner.framework.websocket.PortalWebSocketHandler 约定：
 *   - 发送纯文本 "ping"  → 收到 { type: 'pong' }
 *   - 发送纯文本 "online" → 收到 { type: 'online', payload: '{"onlineCount":n,"clients":[...]}' }
 *   - 其它文本会被服务端记录日志
 */
export function useWebSocket(options: UseWebSocketOptions): UseWebSocketResult {
  const { clientId, autoConnect = false, maxMessages = 200 } = options;

  const socketRef = useRef<WebSocket | null>(null);
  const timerRef = useRef<number | null>(null);
  const retryRef = useRef(0);
  const manualCloseRef = useRef(false);

  const [status, setStatus] = useState<WsStatus>('closed');
  const [messages, setMessages] = useState<WsMessage[]>([]);

  const url = useMemo(() => resolveWsUrl(clientId), [clientId]);

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const push = useCallback(
    (msg: WsMessage) => {
      setMessages((prev) => {
        const next = [...prev, msg];
        return next.length > maxMessages ? next.slice(next.length - maxMessages) : next;
      });
    },
    [maxMessages],
  );

  const disconnect = useCallback(() => {
    manualCloseRef.current = true;
    clearTimer();
    retryRef.current = 0;
    const socket = socketRef.current;
    socketRef.current = null;
    if (socket && socket.readyState <= WebSocket.OPEN) {
      socket.close(1000, 'client disconnect');
    }
    setStatus('closed');
  }, [clearTimer]);

  const connect = useCallback(() => {
    if (socketRef.current && socketRef.current.readyState <= WebSocket.OPEN) {
      return;
    }
    manualCloseRef.current = false;
    clearTimer();
    setStatus('connecting');

    let socket: WebSocket;
    try {
      socket = new WebSocket(url);
    } catch {
      setStatus('error');
      return;
    }
    socketRef.current = socket;

    socket.onopen = () => {
      retryRef.current = 0;
      setStatus('open');
      push({ type: 'system', payload: `已连接到 ${url}`, timestamp: Date.now() });
    };

    socket.onmessage = (event) => {
      const raw = typeof event.data === 'string' ? event.data : '[非文本消息]';
      let parsed: WsMessage;
      try {
        parsed = JSON.parse(raw) as WsMessage;
      } catch {
        parsed = { type: 'text', payload: raw };
      }
      push({ ...parsed, timestamp: parsed.timestamp ?? Date.now() });
    };

    socket.onerror = () => {
      setStatus('error');
    };

    socket.onclose = (event) => {
      socketRef.current = null;
      setStatus('closed');
      push({
        type: 'system',
        payload: `连接已关闭（code=${event.code}${event.reason ? `, reason=${event.reason}` : ''}）`,
        timestamp: Date.now(),
      });

      // 非手动关闭时按退避策略自动重连，最多 5 次
      if (!manualCloseRef.current && retryRef.current < 5) {
        const delay = Math.min(1000 * 2 ** retryRef.current, 15000);
        retryRef.current += 1;
        timerRef.current = window.setTimeout(() => {
          push({ type: 'system', payload: `${delay}ms 后尝试第 ${retryRef.current} 次重连…`, timestamp: Date.now() });
          connect();
        }, delay);
      }
    };
  }, [clearTimer, push, url]);

  const send = useCallback((data: string): boolean => {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      push({ type: 'system', payload: '发送失败：连接未就绪', timestamp: Date.now() });
      return false;
    }
    socket.send(data);
    return true;
  }, [push]);

  const clearMessages = useCallback(() => setMessages([]), []);

  useEffect(() => {
    if (autoConnect) {
      connect();
    }
    return () => {
      manualCloseRef.current = true;
      clearTimer();
      const socket = socketRef.current;
      socketRef.current = null;
      if (socket && socket.readyState <= WebSocket.OPEN) {
        socket.close(1000, 'component unmount');
      }
    };
    // 仅在挂载 / clientId 变化时重建连接
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url]);

  return {
    status,
    messages,
    connected: status === 'open',
    url,
    connect,
    disconnect,
    send,
    clearMessages,
  };
}
