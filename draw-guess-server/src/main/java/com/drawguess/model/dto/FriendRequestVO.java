package com.drawguess.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestVO {

    private Long id;
    private Long fromUserId;
    private String fromNickname;
    private String fromPhone;
    private String status;
    private LocalDateTime createdAt;

    public static FriendRequestVO fromEntity(com.drawguess.model.entity.Friend friend,
                                              com.drawguess.model.entity.User fromUser) {
        FriendRequestVO vo = new FriendRequestVO();
        vo.setId(friend.getId());
        vo.setFromUserId(fromUser.getId());
        vo.setFromNickname(fromUser.getNickname());
        vo.setFromPhone(fromUser.getPhone());
        vo.setStatus(friend.getStatus().name());
        vo.setCreatedAt(friend.getCreatedAt());
        return vo;
    }
}
