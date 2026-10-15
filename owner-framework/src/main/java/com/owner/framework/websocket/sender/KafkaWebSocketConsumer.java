package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.WebSocketLocalDeliverer;
import com.owner.framework.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者：接收广播消息并在本机投递。
 * <p>每个实例使用唯一 groupId 后缀才能收到全量消息，这里通过 clientId 前缀区分。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "kafka")
public class KafkaWebSocketConsumer {

    private final WebSocketLocalDeliverer deliverer;

    @KafkaListener(
            topics = Constants.WS_KAFKA_TOPIC,
            groupId = "#{@webSocketProperties.getInstanceGroupId()}")
    public void onMessage(String payload) {
        WebSocketMessage message = JsonUtils.parse(payload, WebSocketMessage.class);
        if (message != null) {
            deliverer.deliver(message);
        }
    }
}
