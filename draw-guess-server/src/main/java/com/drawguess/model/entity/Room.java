package com.drawguess.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.drawguess.model.enums.RoomState;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomId;

    private Long creatorId;

    @EnumValue
    private RoomState state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
