package com.drawguess.websocket;

import com.drawguess.model.entity.User;
import com.drawguess.service.GameService;
import com.drawguess.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class GameWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final GameService gameService;

    public GameWebSocketHandler(SimpMessagingTemplate messagingTemplate,
                                RoomService roomService,
                                GameService gameService) {
        this.messagingTemplate = messagingTemplate;
        this.roomService = roomService;
        this.gameService = gameService;
    }

    @MessageMapping("/join_room")
    public void joinRoom(@Payload Map<String, String> payload, StompHeaderAccessor accessor) {
        User user = getUser(accessor);
        if (user == null) return;

        String roomId = payload.get("roomId");
        try {
            roomService.joinRoom(roomId, user.getId());
        } catch (Exception e) {
            sendError(accessor, e.getMessage());
        }
    }

    @MessageMapping("/leave_room")
    public void leaveRoom(@Payload Map<String, String> payload, StompHeaderAccessor accessor) {
        User user = getUser(accessor);
        if (user == null) return;

        String roomId = payload.get("roomId");
        try {
            roomService.leaveRoom(roomId, user.getId());
        } catch (Exception e) {
            sendError(accessor, e.getMessage());
        }
    }

    @MessageMapping("/draw")
    public void draw(@Payload Map<String, Object> payload, StompHeaderAccessor accessor) {
        User user = getUser(accessor);
        if (user == null) return;

        String roomId = (String) payload.get("roomId");
        Object drawData = payload.get("data");

        messagingTemplate.convertAndSend("/topic/room/" + roomId,
                WsMessage.event("draw_data",
                        Map.of("userId", user.getId(), "data", drawData)));
    }

    @MessageMapping("/submit_answer")
    public void submitAnswer(@Payload Map<String, String> payload, StompHeaderAccessor accessor) {
        User user = getUser(accessor);
        if (user == null) return;

        String roomId = payload.get("roomId");
        String answer = payload.get("answer");

        try {
            boolean correct = gameService.submitAnswer(roomId, user.getId(), answer);
            if (!correct) {
                // 答错不发错误，Service 内部已广播 answer_result
            }
        } catch (Exception e) {
            sendError(accessor, e.getMessage());
        }
    }

    @MessageMapping("/start_game")
    public void startGame(@Payload Map<String, String> payload, StompHeaderAccessor accessor) {
        User user = getUser(accessor);
        if (user == null) return;

        String roomId = payload.get("roomId");
        try {
            gameService.startGame(roomId, user.getId());
        } catch (Exception e) {
            sendError(accessor, e.getMessage());
        }
    }

    @MessageMapping("/end_round")
    public void endRound(@Payload Map<String, String> payload, StompHeaderAccessor accessor) {
        User user = getUser(accessor);
        if (user == null) return;

        String roomId = payload.get("roomId");
        try {
            // 只有画家能主动结束回合
            gameService.endRound(roomId);
        } catch (Exception e) {
            sendError(accessor, e.getMessage());
        }
    }

    // ---- 辅助方法 ----

    private User getUser(StompHeaderAccessor accessor) {
        Object userObj = accessor.getSessionAttributes().get("user");
        if (userObj instanceof User user) {
            return user;
        }
        log.warn("WebSocket 消息无用户信息");
        return null;
    }

    private void sendError(StompHeaderAccessor accessor, String message) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors",
                    WsMessage.event("error", Map.of("message", message)));
        }
    }
}
