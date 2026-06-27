package com.drawguess.model.dto;

import com.drawguess.model.entity.Word;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WordVO {

    private Long id;
    private String word;
    private Boolean isDefault;
    private LocalDateTime createdAt;

    public static WordVO fromEntity(Word entity) {
        WordVO vo = new WordVO();
        vo.setId(entity.getId());
        vo.setWord(entity.getWord());
        vo.setIsDefault(entity.getIsDefault());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
