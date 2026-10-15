package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.WebSocketMessage;
import com.owner.framework.websocket.WebSocketSendMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 广播方式，适用于多实例部署。
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaWebSocketMessageSender implements WebSocketMessageSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public WebSocketSendMode mode() {
        return WebSocketSendMode.KAFKA;
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
        kafkaTemplate.send(Constants.WS_KAFKA_TOPIC, json);
    }
}
