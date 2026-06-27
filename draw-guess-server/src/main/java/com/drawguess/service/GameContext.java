package com.drawguess.service;

import com.drawguess.model.entity.RoomMember;
import com.drawguess.model.enums.RoomState;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class GameContext {

    private String roomId;
    private RoomState state = RoomState.WAITING;

    /** 画家顺序列表（userId） */
    private List<Long> painterOrder = new ArrayList<>();
    private int currentRoundIndex = 0;

    /** 当前回合信息 */
    private String currentWord;
    private Long currentPainterId;
    private Set<Long> answeredUserIds = new HashSet<>();

    /** 倒计时（秒） */
    private int roundTimeLeft = 0;

    /** 本局各成员得分（userId -> 本局得分） */
    private java.util.Map<Long, Integer> roundScores = new java.util.HashMap<>();

    public boolean isPlaying() {
        return state == RoomState.PLAYING;
    }

    public boolean hasAnswered(Long userId) {
        return answeredUserIds.contains(userId);
    }

    public boolean isPainter(Long userId) {
        return userId.equals(currentPainterId);
    }

    public int getTotalRounds() {
        return painterOrder.size();
    }

    public int getCurrentRoundNumber() {
        return currentRoundIndex + 1;
    }

    public boolean isLastRound() {
        return currentRoundIndex >= painterOrder.size() - 1;
    }

    public void addScore(Long userId, int score) {
        roundScores.merge(userId, score, Integer::sum);
    }

    public void resetRound() {
        currentWord = null;
        currentPainterId = null;
        answeredUserIds.clear();
        roundTimeLeft = 0;
    }

    public void advanceRound() {
        currentRoundIndex++;
        resetRound();
    }
}
