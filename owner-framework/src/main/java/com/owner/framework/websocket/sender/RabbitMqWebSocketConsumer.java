package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.WebSocketLocalDeliverer;
import com.owner.framework.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消费者：接收广播消息并在本机投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "rabbitmq")
public class RabbitMqWebSocketConsumer {

    private final WebSocketLocalDeliverer deliverer;

    @RabbitListener(queues = Constants.WS_RABBITMQ_QUEUE)
    public void onMessage(String payload) {
        WebSocketMessage message = JsonUtils.parse(payload, WebSocketMessage.class);
        if (message != null) {
            deliverer.deliver(message);
        }
    }
}
