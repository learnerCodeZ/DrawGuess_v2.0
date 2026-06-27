package com.drawguess.service;

import com.drawguess.model.dto.WordVO;
import com.drawguess.model.entity.Word;

import java.util.List;

public interface WordService {

    List<WordVO> getAllWords();

    long getWordCount();

    void addWord(String word);

    void batchAddWords(List<String> words);

    void deleteWord(Long wordId, Long operatorId);

    void batchDeleteWords(List<Long> wordIds, Long operatorId);

    Word getRandomWord();
}
