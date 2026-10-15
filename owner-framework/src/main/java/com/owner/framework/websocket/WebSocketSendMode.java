package com.owner.framework.websocket;

import lombok.Getter;

import java.util.Arrays;

/**
 * WebSocket 消息发送方式。
 * <p>通过 {@code owner.websocket.send-mode} 配置切换，默认 {@link #LOCAL}。</p>
 */
@Getter
public enum WebSocketSendMode {

    /** 进程内直发（单机模式，默认） */
    LOCAL("local", "本地直发"),
    /** 通过 Redis 发布订阅广播，多实例场景 */
    REDIS("redis", "Redis 发布订阅"),
    /** 通过 RocketMQ 广播，多实例场景 */
    ROCKETMQ("rocketmq", "RocketMQ 广播"),
    /** 通过 Kafka 广播，多实例场景 */
    KAFKA("kafka", "Kafka 广播"),
    /** 通过 RabbitMQ 广播，多实例场景 */
    RABBITMQ("rabbitmq", "RabbitMQ 广播");

    private final String value;
    private final String desc;

    WebSocketSendMode(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static WebSocketSendMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        String normalized = value.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(mode -> mode.value.equals(normalized) || mode.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(LOCAL);
    }
}
