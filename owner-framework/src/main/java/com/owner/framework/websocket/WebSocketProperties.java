package com.owner.framework.websocket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.UUID;

/**
 * WebSocket 相关配置项，前缀 {@code owner.websocket}。
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "owner.websocket")
public class WebSocketProperties {

    /** 连接路径 */
    private String path = "/ws/portal";

    /** 发送方式：local（默认）/ redis / rocketmq / kafka / rabbitmq */
    private String sendMode = WebSocketSendMode.LOCAL.getValue();

    /** 允许的跨域来源 */
    private String allowedOrigins = "*";

    /** 发送缓冲区大小（字节） */
    private int sendBufferSizeLimit = 512 * 1024;

    /** 空闲超时（毫秒），0 表示不限制 */
    private long idleTimeout = 0L;

    /** Kafka 广播消费组前缀 */
    private String groupId = "owner-websocket";

    /**
     * 实例标识，用于 Kafka 广播消费组（每个实例独立消费全量消息）。
     * 不配置时自动按 主机名+UUID 生成。
     */
    private String instanceId = defaultInstanceId();

    public WebSocketSendMode resolveMode() {
        return WebSocketSendMode.fromValue(sendMode);
    }

    /** 供 {@code @KafkaListener(groupId = "#{@webSocketProperties.getInstanceGroupId()}")} 使用 */
    public String getInstanceGroupId() {
        return groupId + "-" + instanceId;
    }

    private static String defaultInstanceId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "local";
            log.debug("[WebSocket] 获取主机名失败，使用默认值: {}", e.getMessage());
        }
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
