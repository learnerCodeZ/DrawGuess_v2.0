package com.drawguess.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameRecordVO {

    private Long id;
    private Long roomId;
    private String roomCode;
    private LocalDateTime playedAt;
    private List<DetailVO> details;

    @Data
    public static class DetailVO {
        private Long userId;
        private String nickname;
        private Integer score;
        private String word;
    }
}
