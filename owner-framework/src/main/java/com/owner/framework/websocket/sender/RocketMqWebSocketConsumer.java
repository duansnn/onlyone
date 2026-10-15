package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.WebSocketLocalDeliverer;
import com.owner.framework.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费者：接收广播消息并在本机投递。
 * <p>使用 BROADCASTING 消费模式，保证每个实例都能收到同一条消息。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "rocketmq")
@RocketMQMessageListener(
        topic = Constants.WS_ROCKETMQ_TOPIC,
        consumerGroup = "owner-websocket-consumer",
        messageModel = MessageModel.BROADCASTING)
public class RocketMqWebSocketConsumer implements RocketMQListener<String> {

    private final WebSocketLocalDeliverer deliverer;

    @Override
    public void onMessage(String payload) {
        WebSocketMessage message = JsonUtils.parse(payload, WebSocketMessage.class);
        if (message != null) {
            deliverer.deliver(message);
        }
    }
}
