package com.drawguess.controller;

import com.drawguess.common.ApiResponse;
import com.drawguess.model.dto.GameRecordVO;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.entity.User;
import com.drawguess.service.LeaderboardService;
import com.drawguess.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final UserService userService;

    public LeaderboardController(LeaderboardService leaderboardService, UserService userService) {
        this.leaderboardService = leaderboardService;
        this.userService = userService;
    }

    @GetMapping("/leaderboard")
    public ApiResponse<List<UserInfoVO>> getLeaderboard() {
        return ApiResponse.success(leaderboardService.getLeaderboard());
    }

    @GetMapping("/user/games")
    public ApiResponse<List<GameRecordVO>> getUserGameRecords() {
        User currentUser = userService.getCurrentUser();
        return ApiResponse.success(leaderboardService.getUserGameRecords(currentUser.getId()));
    }
}
