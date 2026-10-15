package com.owner.framework.websocket;

import com.owner.common.core.utils.JsonUtils;
import com.owner.common.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门户 WebSocket 处理器。
 * <p>连接地址：{@code ws://localhost:41452/ws/portal?clientId=xxx}</p>
 * <ul>
 *     <li>收到 {@code ping} 返回 {@code pong}</li>
 *     <li>收到 {@code online} 返回当前在线客户端列表</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortalWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_CLIENT_ID = "clientId";

    private final WebSocketSessionHolder sessionHolder;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String clientId = resolveClientId(session);
        session.getAttributes().put(ATTR_CLIENT_ID, clientId);
        sessionHolder.register(clientId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String clientId = clientIdOf(session);
        String payload = message.getPayload();

        if ("ping".equalsIgnoreCase(payload)) {
            sendDirect(session, WebSocketMessage.toClient(clientId, "pong", "pong"));
            return;
        }

        if ("online".equalsIgnoreCase(payload)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("onlineCount", sessionHolder.onlineCount());
            data.put("clients", sessionHolder.onlineClientIds());
            sendDirect(session, WebSocketMessage.toClient(clientId, "online", JsonUtils.toJson(data)));
            return;
        }

        log.info("[WebSocket] 收到客户端消息 clientId={}, payload={}", clientId, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionHolder.remove(clientIdOf(session));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[WebSocket] 传输异常 clientId={}, reason={}", clientIdOf(session), exception.getMessage());
        sessionHolder.remove(clientIdOf(session));
    }

    private void sendDirect(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(JsonUtils.toJson(message)));
                }
            }
        } catch (Exception e) {
            log.warn("[WebSocket] 回复消息失败: {}", e.getMessage());
        }
    }

    private String clientIdOf(WebSocketSession session) {
        Object clientId = session.getAttributes().get(ATTR_CLIENT_ID);
        return clientId == null ? session.getId() : clientId.toString();
    }

    /** 优先取查询参数 clientId，其次取请求头，最后退化为会话 ID */
    private String resolveClientId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null && StringUtils.isNotBlank(uri.getQuery())) {
            for (String pair : uri.getQuery().split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "clientId".equals(kv[0]) && StringUtils.isNotBlank(kv[1])) {
                    return kv[1];
                }
            }
        }
        String headerClientId = session.getHandshakeHeaders().getFirst("clientId");
        if (StringUtils.isNotBlank(headerClientId)) {
            return headerClientId;
        }
        return session.getId();
    }
}
