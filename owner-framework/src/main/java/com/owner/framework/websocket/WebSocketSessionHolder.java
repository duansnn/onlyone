package com.owner.framework.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地（当前 JVM）WebSocket 会话注册表。
 */
@Slf4j
@Component
public class WebSocketSessionHolder {

    /** clientId -> session */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String clientId, WebSocketSession session) {
        WebSocketSession previous = sessions.put(clientId, session);
        if (previous != null && previous.isOpen()) {
            try {
                previous.close();
            } catch (IOException e) {
                log.debug("[WebSocket] 关闭旧会话失败 clientId={}, reason={}", clientId, e.getMessage());
            }
        }
        log.info("[WebSocket] 客户端上线 clientId={}, 当前在线数={}", clientId, sessions.size());
    }

    public void remove(String clientId) {
        if (clientId != null) {
            sessions.remove(clientId);
            log.info("[WebSocket] 客户端下线 clientId={}, 当前在线数={}", clientId, sessions.size());
        }
    }

    public WebSocketSession get(String clientId) {
        return clientId == null ? null : sessions.get(clientId);
    }

    public Set<String> onlineClientIds() {
        return Collections.unmodifiableSet(sessions.keySet());
    }

    public Map<String, WebSocketSession> onlineSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public int onlineCount() {
        return sessions.size();
    }
}
