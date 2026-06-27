package com.drawguess.model.dto;

import com.drawguess.model.enums.UserRole;
import com.drawguess.model.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {

    private Long id;
    private String phone;
    private String nickname;
    private UserRole role;
    private UserStatus status;
    private Integer totalScore;
    private LocalDateTime createdAt;
    private Boolean online;

    public static UserInfoVO fromEntity(com.drawguess.model.entity.User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setTotalScore(user.getTotalScore());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    public void setOnlineStatus(boolean online) {
        this.online = online;
    }
}
