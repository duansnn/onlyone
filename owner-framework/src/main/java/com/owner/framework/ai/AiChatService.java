package com.owner.framework.ai;

import com.owner.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Spring AI 2.0.0 对话服务。
 * <p>默认接入 OpenAI 兼容端点（模型与密钥由 {@code spring.ai.openai.*} 配置）。
 * 未配置密钥时不会阻断应用启动，仅在真正调用时给出清晰提示。</p>
 */
@Slf4j
@Service
public class AiChatService {

    private final ObjectProvider<ChatModel> chatModelProvider;

    private volatile ChatClient chatClient;

    public AiChatService(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    /** AI 能力是否可用 */
    public boolean available() {
        return chatModelProvider.getIfAvailable() != null;
    }

    /** 单轮对话 */
    public String chat(String prompt) {
        return client().prompt().user(prompt).call().content();
    }

    /** 带系统提示词的单轮对话 */
    public String chat(String systemPrompt, String userPrompt) {
        return client().prompt().system(systemPrompt).user(userPrompt).call().content();
    }

    /** 流式对话，逐段回调 */
    public void stream(String prompt, Consumer<String> consumer) {
        client().prompt().user(prompt).stream().content()
                .toIterable()
                .forEach(consumer);
    }

    private ChatClient client() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new ServiceException("AI 能力未启用，请配置 spring.ai.openai.api-key");
        }
        ChatClient local = this.chatClient;
        if (local == null) {
            synchronized (this) {
                local = this.chatClient;
                if (local == null) {
                    local = ChatClient.builder(chatModel).build();
                    this.chatClient = local;
                    log.info("[Spring AI] ChatClient 初始化完成，model={}", chatModel.getClass().getSimpleName());
                }
            }
        }
        return local;
    }
}
