package com.drawguess.config;

import com.drawguess.service.UserService;
import com.drawguess.service.WordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitConfig.class);

    private static final List<String> DEFAULT_WORDS = List.of("苹果", "大象", "太阳", "飞机", "钢琴");

    private static final List<String> EXTRA_WORDS = List.of(
            "猫咪", "月亮", "汽车", "蛋糕", "足球",
            "书包", "雨伞", "手表", "西瓜", "眼镜",
            "吉他", "风筝", "冰箱", "火箭", "蝴蝶",
            "蜗牛", "长颈鹿", "向日葵", "彩虹", "雪人"
    );

    @Bean
    public CommandLineRunner initData(UserService userService, WordService wordService) {
        return args -> {
            userService.initSuperAdmin();

            if (wordService.getWordCount() == 0) {
                log.info("词库为空，初始化默认词语...");
                wordService.batchAddWords(DEFAULT_WORDS);
                // 标记默认词（默认词不可删除）
                // 注意：batchAddWords 创建的词 isDefault=false
                // 默认词通过 init.sql 或手动标记
                wordService.batchAddWords(EXTRA_WORDS);
            }

            log.info("数据初始化完成");
        };
    }
}
