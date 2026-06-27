package com.drawguess.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoundInfo {

    private int roundNumber;
    private int totalRounds;
    private Long painterId;
    private String painterNickname;
    private String word;
    private int wordLength;
    private int timeLeft;
    private List<Long> answeredUserIds;
}
