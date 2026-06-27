package com.drawguess.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.drawguess.model.enums.FriendStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend")
public class Friend {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long friendId;

    @EnumValue
    private FriendStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
