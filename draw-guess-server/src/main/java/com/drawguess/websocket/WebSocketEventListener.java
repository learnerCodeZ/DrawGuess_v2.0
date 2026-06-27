package com.drawguess.websocket;

import com.drawguess.model.entity.User;
import com.drawguess.service.OnlineUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final OnlineUserService onlineUserService;

    public WebSocketEventListener(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Object userObj = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("user") : null;
        if (userObj instanceof User user) {
            onlineUserService.userOnline(user.getId());
            log.info("WebSocket 连接: userId={}, online={}", user.getId(), onlineUserService.getOnlineCount());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Object userObj = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("user") : null;
        if (userObj instanceof User user) {
            onlineUserService.userOffline(user.getId());
            log.info("WebSocket 断开: userId={}, online={}", user.getId(), onlineUserService.getOnlineCount());
        }
    }
}
