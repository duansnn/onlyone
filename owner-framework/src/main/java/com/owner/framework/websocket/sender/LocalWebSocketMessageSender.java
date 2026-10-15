package com.owner.framework.websocket.sender;

import com.owner.framework.websocket.WebSocketLocalDeliverer;
import com.owner.framework.websocket.WebSocketMessage;
import com.owner.framework.websocket.WebSocketSendMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地直发：不经过任何中间件，直接把消息推给当前 JVM 上的连接。
 * <p>适用于单实例部署。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LocalWebSocketMessageSender implements WebSocketMessageSender {

    private final WebSocketLocalDeliverer deliverer;

    @Override
    public WebSocketSendMode mode() {
        return WebSocketSendMode.LOCAL;
    }

    @Override
    public void broadcast(WebSocketMessage message) {
        message.setClientId(null);
        deliverer.deliver(message);
    }

    @Override
    public void sendToClient(WebSocketMessage message) {
        deliverer.deliver(message);
    }
}
