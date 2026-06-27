package com.drawguess.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GameResultVO {

    private String roomId;
    private List<PlayerScore> rankings;
    private Map<Long, Integer> scoreChanges;

    @Data
    public static class PlayerScore {
        private Long userId;
        private String nickname;
        private int gameScore;
        private int totalScore;
    }
}
