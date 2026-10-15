package com.owner.common.core.constant;

/**
 * 全局常量。
 */
public interface Constants {

    /** UTF-8 编码 */
    String UTF8 = "UTF-8";

    /** 成功标记 */
    String SUCCESS = "0";

    /** 失败标记 */
    String FAIL = "1";

    /** 是 */
    String YES = "Y";

    /** 否 */
    String NO = "N";

    /** 逻辑删除：未删除 */
    Integer NOT_DELETED = 0;

    /** 逻辑删除：已删除 */
    Integer DELETED = 1;

    /** 超级管理员标识 */
    Long SUPER_ADMIN_ID = 1L;

    /** 默认页大小 */
    int DEFAULT_PAGE_SIZE = 10;

    /** 网关/链路追踪请求头 */
    String HEADER_TRACE_ID = "X-Trace-Id";

    /** 认证请求头 */
    String HEADER_AUTHORIZATION = "Authorization";

    /** Token 前缀 */
    String TOKEN_PREFIX = "Bearer ";

    /** Redis Key 前缀 */
    String REDIS_PREFIX = "owner:";

    /** WebSocket 会话缓存前缀 */
    String WS_SESSION_PREFIX = REDIS_PREFIX + "ws:session:";

    /** WebSocket 广播频道 */
    String WS_BROADCAST_CHANNEL = "owner:ws:broadcast";

    /** WebSocket 广播 RocketMQ Topic */
    String WS_ROCKETMQ_TOPIC = "OWNER_WEBSOCKET_BROADCAST";

    /** WebSocket 广播 Kafka Topic */
    String WS_KAFKA_TOPIC = "owner-websocket-broadcast";

    /** WebSocket 广播 RabbitMQ Exchange */
    String WS_RABBITMQ_EXCHANGE = "owner.websocket.broadcast";

    /** WebSocket 广播 RabbitMQ Queue */
    String WS_RABBITMQ_QUEUE = "owner.websocket.broadcast.queue";
}
