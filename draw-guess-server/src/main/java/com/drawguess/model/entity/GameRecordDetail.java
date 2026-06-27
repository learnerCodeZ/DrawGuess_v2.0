package com.drawguess.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("game_record_detail")
public class GameRecordDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long userId;

    private Integer score;

    private String word;
}
