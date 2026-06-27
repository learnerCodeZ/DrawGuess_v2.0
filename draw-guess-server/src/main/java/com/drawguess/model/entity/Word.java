package com.drawguess.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("word")
public class Word {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    private Boolean isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
