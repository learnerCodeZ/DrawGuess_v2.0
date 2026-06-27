package com.drawguess.model.dto;

import com.drawguess.model.enums.RoomState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameRoomVO {

    private Long id;
    private String roomId;
    private Long creatorId;
    private String creatorNickname;
    private RoomState state;
    private LocalDateTime createdAt;
    private List<MemberInfo> members;

    @Data
    public static class MemberInfo {
        private Long userId;
        private String nickname;
        private Integer score;
        private Integer painterOrder;
    }
}
