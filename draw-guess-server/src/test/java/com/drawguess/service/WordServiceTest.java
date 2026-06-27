package com.drawguess.service;

import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.mapper.WordMapper;
import com.drawguess.model.dto.WordVO;
import com.drawguess.model.entity.Word;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WordServiceTest {

    @Autowired
    private WordService wordService;

    @Autowired
    private WordMapper wordMapper;

    @AfterEach
    void cleanup() {
        // 删除测试中可能添加的非默认词
        var words = wordService.getAllWords();
        words.stream()
                .filter(w -> !w.getIsDefault() && w.getWord().startsWith("_test_"))
                .forEach(w -> wordMapper.deleteById(w.getId()));
    }

    @Test
    @Order(1)
    void testGetAllWords() {
        var words = wordService.getAllWords();
        assertFalse(words.isEmpty());
        // 应有5个默认词
        long defaultCount = words.stream().filter(WordVO::getIsDefault).count();
        assertEquals(5, defaultCount);
    }

    @Test
    @Order(2)
    void testGetWordCount() {
        long count = wordService.getWordCount();
        assertTrue(count >= 5);
    }

    @Test
    @Order(3)
    void testAddWord() {
        wordService.addWord("_test_单元测试");
        var words = wordService.getAllWords();
        assertTrue(words.stream().anyMatch(w -> "_test_单元测试".equals(w.getWord())));
    }

    @Test
    @Order(4)
    void testAddWord_Duplicate() {
        BusinessException e = assertThrows(BusinessException.class, () -> {
            wordService.addWord("苹果"); // 默认词已存在
        });
        assertEquals(ResultCode.WORD_EXISTS, e.getResultCode());
    }

    @Test
    @Order(5)
    void testDeleteDefaultWord() {
        var defaultWord = wordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Word>()
                        .eq(Word::getIsDefault, true).last("LIMIT 1")
        );
        if (defaultWord != null) {
            BusinessException e = assertThrows(BusinessException.class, () -> {
                wordService.deleteWord(defaultWord.getId(), 1L);
            });
            assertEquals(ResultCode.DEFAULT_WORD_CANNOT_DELETE, e.getResultCode());
        }
    }

    @Test
    @Order(6)
    void testBatchAddWords() {
        wordService.batchAddWords(java.util.List.of("_test_批量1", "_test_批量2", "_test_批量3"));
        var words = wordService.getAllWords();
        long testCount = words.stream().filter(w -> w.getWord().startsWith("_test_")).count();
        // 包含之前的 _test_单元测试
        assertTrue(testCount >= 4);
    }

    @Test
    @Order(7)
    void testGetRandomWord() {
        Word word = wordService.getRandomWord();
        assertNotNull(word);
        assertNotNull(word.getWord());
    }

    @Test
    @Order(8)
    void testDeleteCustomWord() {
        wordService.addWord("_test_待删除");
        var words = wordService.getAllWords();
        var toDelete = words.stream()
                .filter(w -> "_test_待删除".equals(w.getWord()))
                .findFirst().orElse(null);
        assertNotNull(toDelete);
        assertDoesNotThrow(() -> wordService.deleteWord(toDelete.getId(), 1L));
    }
}
