package com.owner.framework.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * WebSocket 传输的消息体。
 * <p>该对象会被序列化为 JSON 在 Redis / RocketMQ / Kafka / RabbitMQ 之间流转。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息类型，如 notice / heartbeat / broadcast */
    private String type;

    /** 目标客户端标识；为空表示广播给所有在线连接 */
    private String clientId;

    /** 消息内容，通常为 JSON 字符串 */
    private String payload;

    /** 发送时间戳（毫秒） */
    private Long timestamp;

    public static WebSocketMessage broadcast(String type, String payload) {
        return WebSocketMessage.builder()
                .type(type)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static WebSocketMessage toClient(String clientId, String type, String payload) {
        return WebSocketMessage.builder()
                .type(type)
                .clientId(clientId)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 是否为广播消息 */
    public boolean isBroadcast() {
        return clientId == null || clientId.isBlank();
    }
}
