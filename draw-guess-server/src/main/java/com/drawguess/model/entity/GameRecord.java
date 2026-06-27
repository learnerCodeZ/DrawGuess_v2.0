package com.drawguess.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_record")
public class GameRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime playedAt;
}
