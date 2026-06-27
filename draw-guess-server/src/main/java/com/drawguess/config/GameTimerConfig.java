package com.drawguess.config;

import com.drawguess.service.GameContext;
import com.drawguess.service.GameService;
import com.drawguess.websocket.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

@Configuration
@EnableScheduling
public class GameTimerConfig {

    private static final Logger log = LoggerFactory.getLogger(GameTimerConfig.class);

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameTimerConfig(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 每秒检查所有进行中的游戏，倒计时并广播
     */
    @Scheduled(fixedRate = 1000)
    public void tickGameTimers() {
        // 遍历所有活跃的游戏上下文
        for (String roomId : gameService.getActiveRoomIds()) {
            GameContext ctx = gameService.getGameContext(roomId);
            if (ctx == null || !ctx.isPlaying()) continue;

            int timeLeft = ctx.getRoundTimeLeft();
            if (timeLeft <= 0) continue;

            ctx.setRoundTimeLeft(timeLeft - 1);

            // 广播倒计时
            broadcast(roomId, "tick", Map.of(
                    "timeLeft", ctx.getRoundTimeLeft(),
                    "roundNumber", ctx.getCurrentRoundNumber()
            ));

            // 时间到，自动结束回合
            if (ctx.getRoundTimeLeft() <= 0) {
                log.info("回合倒计时结束: roomId={}", roomId);
                try {
                    gameService.endRound(roomId);
                } catch (Exception e) {
                    log.warn("自动结束回合失败: roomId={}, error={}", roomId, e.getMessage());
                }
            }
        }
    }

    private void broadcast(String roomId, String event, Object data) {
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, WsMessage.event(event, data));
        } catch (Exception e) {
            log.warn("广播失败: roomId={}, event={}", roomId, event);
        }
    }
}
