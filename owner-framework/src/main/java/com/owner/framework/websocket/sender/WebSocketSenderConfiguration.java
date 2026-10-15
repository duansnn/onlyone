package com.owner.framework.websocket.sender;

import com.owner.framework.websocket.WebSocketLocalDeliverer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 按 {@code owner.websocket.send-mode} 装配唯一的 {@link WebSocketMessageSender}。
 * <p>取值：{@code local}（默认，缺省即生效）/ {@code redis} / {@code rocketmq} / {@code kafka} / {@code rabbitmq}</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class WebSocketSenderConfiguration {

    private static final String MODE_KEY = "owner.websocket.send-mode";

    /** 默认：本地直发，单实例部署 */
    @Bean
    @ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode",
            havingValue = "local", matchIfMissing = true)
    public WebSocketMessageSender localWebSocketMessageSender(WebSocketLocalDeliverer deliverer) {
        log.info("[WebSocket] 发送方式 = local（本地直发）");
        return new LocalWebSocketMessageSender(deliverer);
    }

    /** Redis 发布订阅 */
    @Bean
    @ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "redis")
    public WebSocketMessageSender redisWebSocketMessageSender(RedissonClient redissonClient,
                                                             WebSocketLocalDeliverer deliverer) {
        return new RedisWebSocketMessageSender(redissonClient, deliverer);
    }

    /** RocketMQ 广播 */
    @Bean
    @ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "rocketmq")
    public WebSocketMessageSender rocketMqWebSocketMessageSender(
            ObjectProvider<RocketMQTemplate> templateProvider) {
        RocketMQTemplate template = templateProvider.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException(MODE_KEY + "=rocketmq 时必须配置 rocketmq.name-server 与 rocketmq.producer.group");
        }
        log.info("[WebSocket] 发送方式 = rocketmq");
        return new RocketMqWebSocketMessageSender(template);
    }

    /** Kafka 广播 */
    @Bean
    @ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "kafka")
    public WebSocketMessageSender kafkaWebSocketMessageSender(
            ObjectProvider<KafkaTemplate<String, String>> templateProvider) {
        KafkaTemplate<String, String> template = templateProvider.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException(MODE_KEY + "=kafka 时必须配置 spring.kafka.bootstrap-servers");
        }
        log.info("[WebSocket] 发送方式 = kafka");
        return new KafkaWebSocketMessageSender(template);
    }

    /** RabbitMQ 广播 */
    @Bean
    @ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "rabbitmq")
    public WebSocketMessageSender rabbitMqWebSocketMessageSender(RabbitTemplate rabbitTemplate) {
        log.info("[WebSocket] 发送方式 = rabbitmq");
        return new RabbitMqWebSocketMessageSender(rabbitTemplate);
    }
}
