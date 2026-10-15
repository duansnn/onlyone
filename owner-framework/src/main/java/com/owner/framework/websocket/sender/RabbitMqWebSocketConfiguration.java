package com.owner.framework.websocket.sender;

import com.owner.common.core.constant.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 广播拓扑声明：Fanout Exchange + 匿名自动删除队列，保证每个实例都能收到广播。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "owner.websocket", name = "send-mode", havingValue = "rabbitmq")
public class RabbitMqWebSocketConfiguration {

    @Bean
    public FanoutExchange webSocketBroadcastExchange() {
        return new FanoutExchange(Constants.WS_RABBITMQ_EXCHANGE, false, false);
    }

    @Bean
    public Queue webSocketBroadcastQueue() {
        return new Queue(Constants.WS_RABBITMQ_QUEUE, false, false, true);
    }

    @Bean
    public Binding webSocketBroadcastBinding(Queue webSocketBroadcastQueue,
                                             FanoutExchange webSocketBroadcastExchange) {
        return BindingBuilder.bind(webSocketBroadcastQueue).to(webSocketBroadcastExchange);
    }
}
