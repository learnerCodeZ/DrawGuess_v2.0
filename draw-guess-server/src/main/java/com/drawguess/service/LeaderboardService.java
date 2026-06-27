package com.drawguess.service;

import com.drawguess.model.dto.GameRecordVO;
import com.drawguess.model.dto.UserInfoVO;

import java.util.List;

public interface LeaderboardService {

    List<UserInfoVO> getLeaderboard();

    List<GameRecordVO> getUserGameRecords(Long userId);

    void invalidateLeaderboardCache();
}
