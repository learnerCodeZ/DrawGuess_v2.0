package com.drawguess.websocket;

import com.drawguess.model.entity.User;
import com.drawguess.model.enums.UserStatus;
import com.drawguess.security.JwtTokenProvider;
import com.drawguess.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (!StringUtils.hasText(token)) {
                // 也尝试从 Header 中获取
                token = servletRequest.getServletRequest().getHeader("Authorization");
                if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                try {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    User user = userService.getUserById(userId);
                    if (user != null && user.getStatus() == UserStatus.APPROVED) {
                        attributes.put("user", user);
                        attributes.put("userId", user.getId());
                        log.info("WebSocket 认证成功: userId={}", user.getId());
                        return true;
                    }
                } catch (Exception e) {
                    log.warn("WebSocket 认证失败: {}", e.getMessage());
                }
            }
        }

        log.warn("WebSocket 握手拒绝: 认证失败");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
