package com.drawguess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.mapper.WordMapper;
import com.drawguess.model.dto.WordVO;
import com.drawguess.model.entity.Word;
import com.drawguess.service.WordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class WordServiceImpl implements WordService {

    private static final Logger log = LoggerFactory.getLogger(WordServiceImpl.class);

    private final WordMapper wordMapper;
    private final Random random = new Random();

    public WordServiceImpl(WordMapper wordMapper) {
        this.wordMapper = wordMapper;
    }

    @Override
    public List<WordVO> getAllWords() {
        List<Word> words = wordMapper.selectList(
                new LambdaQueryWrapper<Word>().orderByAsc(Word::getId)
        );
        return words.stream().map(WordVO::fromEntity).collect(Collectors.toList());
    }

    @Override
    public long getWordCount() {
        return wordMapper.selectCount(null);
    }

    @Override
    public void addWord(String word) {
        String trimmed = word.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "词语不能为空");
        }

        Word existing = wordMapper.selectOne(
                new LambdaQueryWrapper<Word>().eq(Word::getWord, trimmed)
        );
        if (existing != null) {
            throw new BusinessException(ResultCode.WORD_EXISTS);
        }

        Word entity = new Word();
        entity.setWord(trimmed);
        entity.setIsDefault(false);
        wordMapper.insert(entity);
        log.info("添加词语: {}", trimmed);
    }

    @Override
    public void batchAddWords(List<String> words) {
        int added = 0;
        int skipped = 0;
        for (String w : words) {
            String trimmed = w.trim();
            if (trimmed.isEmpty()) continue;

            Word existing = wordMapper.selectOne(
                    new LambdaQueryWrapper<Word>().eq(Word::getWord, trimmed)
            );
            if (existing != null) {
                skipped++;
                continue;
            }

            Word entity = new Word();
            entity.setWord(trimmed);
            entity.setIsDefault(false);
            wordMapper.insert(entity);
            added++;
        }
        log.info("批量添加词语: added={}, skipped={}", added, skipped);
    }

    @Override
    public void deleteWord(Long wordId, Long operatorId) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (word.getIsDefault()) {
            throw new BusinessException(ResultCode.DEFAULT_WORD_CANNOT_DELETE);
        }
        wordMapper.deleteById(wordId);
        log.info("删除词语: wordId={}, operatorId={}", wordId, operatorId);
    }

    @Override
    public void batchDeleteWords(List<Long> wordIds, Long operatorId) {
        int deleted = 0;
        int skipped = 0;
        for (Long wordId : wordIds) {
            Word word = wordMapper.selectById(wordId);
            if (word == null || word.getIsDefault()) {
                skipped++;
                continue;
            }
            wordMapper.deleteById(wordId);
            deleted++;
        }
        log.info("批量删除词语: deleted={}, skipped={}, operatorId={}", deleted, skipped, operatorId);
    }

    @Override
    public Word getRandomWord() {
        List<Word> words = wordMapper.selectList(null);
        if (words.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "词库为空");
        }
        return words.get(random.nextInt(words.size()));
    }
}
