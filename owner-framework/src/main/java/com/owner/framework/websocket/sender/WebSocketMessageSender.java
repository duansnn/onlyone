package com.owner.framework.websocket.sender;

import com.owner.framework.websocket.WebSocketMessage;
import com.owner.framework.websocket.WebSocketSendMode;

/**
 * WebSocket 消息发送策略。当前生效的实现由 {@code owner.websocket.send-mode} 决定。
 */
public interface WebSocketMessageSender {

    /** 当前策略对应的发送方式 */
    WebSocketSendMode mode();

    /** 广播给所有在线连接 */
    void broadcast(WebSocketMessage message);

    /** 向指定客户端发送 */
    void sendToClient(WebSocketMessage message);
}
