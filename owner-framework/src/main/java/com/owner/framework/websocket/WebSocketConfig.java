package com.owner.framework.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册。
 * <p>握手地址：{@code ws://<host>:41452/ws/portal?clientId=xxx}</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PortalWebSocketHandler portalWebSocketHandler;
    private final WebSocketProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(portalWebSocketHandler, properties.getPath())
                .setAllowedOriginPatterns(properties.getAllowedOrigins().split(","));
        log.info("[WebSocket] 端点已注册 path={}, sendMode={}",
                properties.getPath(), properties.resolveMode().getDesc());
    }
}
