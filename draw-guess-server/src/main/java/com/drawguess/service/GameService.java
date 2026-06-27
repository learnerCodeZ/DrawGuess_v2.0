package com.drawguess.service;

import com.drawguess.model.dto.RoundInfo;

public interface GameService {

    void startGame(String roomId, Long userId);

    RoundInfo startRound(String roomId);

    RoundInfo getCurrentRound(String roomId);

    boolean submitAnswer(String roomId, Long userId, String answer);

    void endRound(String roomId);

    void endGame(String roomId);

    GameContext getGameContext(String roomId);

    boolean isGamePlaying(String roomId);

    java.util.Set<String> getActiveRoomIds();
}
