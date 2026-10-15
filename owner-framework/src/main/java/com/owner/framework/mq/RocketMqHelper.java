package com.owner.framework.mq;

import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 发送助手（RocketMQ Spring 2.3.6）。
 * <p>仅当配置了 {@code rocketmq.name-server} 时 {@link RocketMQTemplate} 才会存在，
 * 因此这里用 {@link ObjectProvider} 做优雅降级，未启用时给出明确提示。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMqHelper {

    private final ObjectProvider<RocketMQTemplate> templateProvider;

    /** RocketMQ 是否已启用 */
    public boolean available() {
        return templateProvider.getIfAvailable() != null;
    }

    /** 同步发送 */
    public SendResult syncSend(String topic, Object payload) {
        return requireTemplate().syncSend(topic, MessageBuilder.withPayload(toJson(payload)).build());
    }

    /** 同步发送，并指定超时（毫秒） */
    public SendResult syncSend(String topic, Object payload, long timeoutMillis) {
        return requireTemplate().syncSend(topic,
                MessageBuilder.withPayload(toJson(payload)).build(), timeoutMillis);
    }

    /** 顺序发送 */
    public SendResult syncSendOrderly(String topic, Object payload, String hashKey) {
        return requireTemplate().syncSendOrderly(topic,
                MessageBuilder.withPayload(toJson(payload)).build(), hashKey);
    }

    /** 异步发送 */
    public void asyncSend(String topic, Object payload) {
        requireTemplate().asyncSend(topic, MessageBuilder.withPayload(toJson(payload)).build(),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.debug("[RocketMQ] 异步发送成功 topic={}, msgId={}", topic, sendResult.getMsgId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("[RocketMQ] 异步发送失败 topic={}", topic, e);
                    }
                });
    }

    /** 单向发送，不关心结果 */
    public void sendOneWay(String topic, Object payload) {
        requireTemplate().sendOneWay(topic, MessageBuilder.withPayload(toJson(payload)).build());
    }

    /** 延时发送，delayLevel 从 1 开始（1s 5s 10s 30s 1m ...） */
    public SendResult sendDelay(String topic, Object payload, int delayLevel) {
        return requireTemplate().syncSend(topic,
                MessageBuilder.withPayload(toJson(payload)).setHeader("DELAY", delayLevel).build());
    }

    private RocketMQTemplate requireTemplate() {
        RocketMQTemplate template = templateProvider.getIfAvailable();
        if (template == null) {
            throw new ServiceException("RocketMQ 未启用，请先配置 rocketmq.name-server 与 rocketmq.producer.group");
        }
        return template;
    }

    private String toJson(Object payload) {
        return payload instanceof String str ? str : JsonUtils.toJson(payload);
    }
}
