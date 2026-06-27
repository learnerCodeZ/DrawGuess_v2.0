package com.drawguess.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.drawguess.model.enums.UserRole;
import com.drawguess.model.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String nickname;

    private String password;

    @EnumValue
    private UserRole role;

    @EnumValue
    private UserStatus status;

    private Integer totalScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
