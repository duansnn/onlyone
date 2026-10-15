package com.owner.system.controller;

import com.owner.common.core.domain.R;
import com.owner.framework.ai.AiChatService;
import com.owner.framework.quartz.PortalHeartbeatJob;
import com.owner.framework.websocket.WebSocketBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门户运行状态与能力演示接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Tag(name = "门户运行状态", description = "中间件连通状态、WebSocket 推送与 AI 能力演示")
public class PortalController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Environment environment;
    private final WebSocketBroadcastService webSocketBroadcastService;
    private final AiChatService aiChatService;

    @Value("${spring.application.name:owner-server}")
    private String applicationName;

    @Value("${server.port:41452}")
    private int serverPort;

    @Operation(summary = "应用信息与运行状态")
    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applicationName", applicationName);
        data.put("port", serverPort);
        data.put("activeProfiles", String.join(",", environment.getActiveProfiles()));
        data.put("serverTime", FORMATTER.format(LocalDateTime.now()));
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("websocketSendMode", webSocketBroadcastService.sendMode().getValue());
        data.put("websocketOnlineCount", webSocketBroadcastService.onlineCount());
        data.put("websocketOnlineClients", webSocketBroadcastService.onlineClientIds());
        data.put("quartzHeartbeatExecutions", PortalHeartbeatJob.executionCount());
        data.put("aiAvailable", aiChatService.available());
        return R.ok(data);
    }

    @Operation(summary = "广播 WebSocket 消息", description = "按 owner.websocket.send-mode 配置的方式发送")
    @PostMapping("/websocket/broadcast")
    public R<Map<String, Object>> broadcast(
            @Parameter(description = "消息内容") @RequestParam String content) {
        webSocketBroadcastService.broadcast(WebSocketBroadcastService.TYPE_BROADCAST, content);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sendMode", webSocketBroadcastService.sendMode().getValue());
        data.put("content", content);
        return R.ok("广播已提交", data);
    }

    @Operation(summary = "定向推送 WebSocket 消息")
    @PostMapping("/websocket/send")
    public R<Void> sendToClient(
            @Parameter(description = "目标客户端 ID") @RequestParam String clientId,
            @Parameter(description = "消息内容") @RequestParam String content) {
        webSocketBroadcastService.sendToClient(clientId, WebSocketBroadcastService.TYPE_NOTICE, content);
        return R.ok();
    }

    @Operation(summary = "AI 对话（Spring AI）", description = "需要配置 spring.ai.openai.api-key")
    @GetMapping("/ai/chat")
    public R<String> chat(@Parameter(description = "提问内容") @RequestParam String prompt) {
        return R.ok(aiChatService.chat(prompt));
    }
}
