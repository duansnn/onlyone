package com.owner.framework.websocket;

import com.owner.common.core.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * 本地投递器：把消息真正写入当前 JVM 的 WebSocket 连接。
 * <p>无论使用哪种发送方式（local / redis / rocketmq / kafka / rabbitmq），
 * 最终都会落到这里完成最后一次投递。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketLocalDeliverer {

    private final WebSocketSessionHolder sessionHolder;

    /**
     * 投递消息：{@code clientId} 为空则广播，否则定向下发。
     */
    public int deliver(WebSocketMessage message) {
        if (message == null) {
            return 0;
        }
        String text = JsonUtils.toJson(message);
        if (text == null) {
            return 0;
        }
        TextMessage textMessage = new TextMessage(text);

        if (message.isBroadcast()) {
            int success = 0;
            for (Map.Entry<String, WebSocketSession> entry : sessionHolder.onlineSessions().entrySet()) {
                if (send(entry.getKey(), entry.getValue(), textMessage)) {
                    success++;
                }
            }
            log.debug("[WebSocket] 广播消息 type={}, 成功投递 {} 个连接", message.getType(), success);
            return success;
        }

        WebSocketSession session = sessionHolder.get(message.getClientId());
        if (session == null) {
            log.warn("[WebSocket] 目标客户端不在线，消息被丢弃 clientId={}, type={}",
                    message.getClientId(), message.getType());
            return 0;
        }
        return send(message.getClientId(), session, textMessage) ? 1 : 0;
    }

    private boolean send(String clientId, WebSocketSession session, TextMessage textMessage) {
        if (session == null || !session.isOpen()) {
            sessionHolder.remove(clientId);
            return false;
        }
        try {
            // WebSocketSession 非线程安全，发送时加锁
            synchronized (session) {
                session.sendMessage(textMessage);
            }
            return true;
        } catch (IOException e) {
            log.warn("[WebSocket] 发送失败 clientId={}, reason={}", clientId, e.getMessage());
            sessionHolder.remove(clientId);
            return false;
        }
    }
}
