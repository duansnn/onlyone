package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.WebSocketLocalDeliverer;
import com.owner.framework.websocket.WebSocketMessage;
import com.owner.framework.websocket.WebSocketSendMode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;

/**
 * Redis 发布订阅方式（Redisson RTopic 实现），适用于多实例部署。
 */
@Slf4j
public class RedisWebSocketMessageSender implements WebSocketMessageSender {

    private final RedissonClient redissonClient;
    private final WebSocketLocalDeliverer deliverer;
    private final RTopic topic;

    public RedisWebSocketMessageSender(RedissonClient redissonClient, WebSocketLocalDeliverer deliverer) {
        this.redissonClient = redissonClient;
        this.deliverer = deliverer;
        this.topic = redissonClient.getTopic(Constants.WS_BROADCAST_CHANNEL, JsonJacksonCodec.INSTANCE);
        // 订阅本频道，收到其他实例发布的消息后在本机投递
        this.topic.addListener(String.class, (channel, msg) -> {
            WebSocketMessage message = JsonUtils.parse(msg, WebSocketMessage.class);
            if (message != null) {
                deliverer.deliver(message);
            }
        });
        log.info("[WebSocket] 已启用 Redis 发布订阅发送方式，channel={}", Constants.WS_BROADCAST_CHANNEL);
    }

    @Override
    public WebSocketSendMode mode() {
        return WebSocketSendMode.REDIS;
    }

    @Override
    public void broadcast(WebSocketMessage message) {
        message.setClientId(null);
        publish(message);
    }

    @Override
    public void sendToClient(WebSocketMessage message) {
        publish(message);
    }

    private void publish(WebSocketMessage message) {
        String json = JsonUtils.toJson(message);
        if (json == null) {
            return;
        }
        topic.publish(json);
    }

    /** 供健康检查展示当前使用的 Redisson 客户端信息 */
    public RedissonClient redissonClient() {
        return redissonClient;
    }
}
