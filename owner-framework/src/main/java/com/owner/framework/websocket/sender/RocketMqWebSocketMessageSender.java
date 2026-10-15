package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.WebSocketMessage;
import com.owner.framework.websocket.WebSocketSendMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;

/**
 * RocketMQ 广播方式（RocketMQ Spring 2.3.6），适用于多实例部署。
 */
@Slf4j
@RequiredArgsConstructor
public class RocketMqWebSocketMessageSender implements WebSocketMessageSender {

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public WebSocketSendMode mode() {
        return WebSocketSendMode.ROCKETMQ;
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
        rocketMQTemplate.syncSend(Constants.WS_ROCKETMQ_TOPIC,
                MessageBuilder.withPayload(json).build());
    }
}
