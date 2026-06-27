package com.drawguess.controller;

import com.drawguess.common.ApiResponse;
import com.drawguess.common.ResultCode;
import com.drawguess.model.dto.RoundInfo;
import com.drawguess.model.entity.User;
import com.drawguess.service.GameService;
import com.drawguess.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;
    private final UserService userService;

    public GameController(GameService gameService, UserService userService) {
        this.gameService = gameService;
        this.userService = userService;
    }

    @PostMapping("/start")
    public ApiResponse<Void> startGame(@RequestBody Map<String, String> body) {
        User currentUser = userService.getCurrentUser();
        gameService.startGame(body.get("roomId"), currentUser.getId());
        return ApiResponse.success("游戏开始");
    }

    @GetMapping("/round")
    public ApiResponse<RoundInfo> getCurrentRound(@RequestParam String roomId) {
        RoundInfo round = gameService.getCurrentRound(roomId);
        if (round == null) {
            return ApiResponse.error(ResultCode.ROOM_NOT_FOUND.getCode(), "没有进行中的游戏");
        }
        // 非画家不能看到答案
        User currentUser = userService.getCurrentUser();
        if (!round.getPainterId().equals(currentUser.getId())) {
            round.setWord(null);
        }
        return ApiResponse.success(round);
    }

    @PostMapping("/answer")
    public ApiResponse<Map<String, Object>> submitAnswer(@RequestBody Map<String, String> body) {
        User currentUser = userService.getCurrentUser();
        boolean correct = gameService.submitAnswer(
                body.get("roomId"), currentUser.getId(), body.get("answer"));
        return ApiResponse.success(Map.of("correct", correct));
    }

    @PostMapping("/end-round")
    public ApiResponse<Void> endRound(@RequestBody Map<String, String> body) {
        gameService.endRound(body.get("roomId"));
        return ApiResponse.success("回合结束");
    }
}
