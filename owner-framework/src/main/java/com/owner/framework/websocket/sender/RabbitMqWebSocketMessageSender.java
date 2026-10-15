package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.WebSocketMessage;
import com.owner.framework.websocket.WebSocketSendMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ 广播方式（Fanout Exchange），适用于多实例部署。
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitMqWebSocketMessageSender implements WebSocketMessageSender {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public WebSocketSendMode mode() {
        return WebSocketSendMode.RABBITMQ;
    }

    @Override
    public void broadcast(WebSocketMessage message) {
        message.setClientId(null);
        send(message);
    }

    @Override
    public void sendToClient(WebSocketMessage message) {
        send(message);
    }

    private void send(WebSocketMessage message) {
        String json = JsonUtils.toJson(message);
        if (json == null) {
            return;
        }
        // fanout exchange 忽略 routingKey
        rabbitTemplate.convertAndSend(Constants.WS_RABBITMQ_EXCHANGE, "", json);
    }
}
