package com.owner.framework.websocket;

import com.owner.common.core.utils.JsonUtils;
import com.owner.framework.websocket.sender.WebSocketMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * WebSocket 推送门面：业务代码只需调用这里，具体走哪种发送方式由配置决定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

    public static final String TYPE_NOTICE = "notice";
    public static final String TYPE_BROADCAST = "broadcast";
    public static final String TYPE_TODO = "todo";

    private final WebSocketMessageSender sender;
    private final WebSocketSessionHolder sessionHolder;

    /** 广播（对象会被序列化为 JSON） */
    public void broadcast(String type, Object data) {
        String payload = data instanceof String str ? str : JsonUtils.toJson(data);
        sender.broadcast(WebSocketMessage.broadcast(type, payload));
        log.debug("[WebSocket] 已广播 type={}, mode={}", type, sender.mode().getValue());
    }

    /** 定向推送 */
    public void sendToClient(String clientId, String type, Object data) {
        String payload = data instanceof String str ? str : JsonUtils.toJson(data);
        sender.sendToClient(WebSocketMessage.toClient(clientId, type, payload));
        log.debug("[WebSocket] 已定向发送 clientId={}, type={}", clientId, type);
    }

    /** 当前发送方式 */
    public WebSocketSendMode sendMode() {
        return sender.mode();
    }

    /** 当前在线客户端 */
    public Set<String> onlineClientIds() {
        return sessionHolder.onlineClientIds();
    }

    /** 当前在线连接数 */
    public int onlineCount() {
        return sessionHolder.onlineCount();
    }
}
