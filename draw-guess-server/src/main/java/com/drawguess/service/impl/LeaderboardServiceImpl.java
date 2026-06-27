package com.drawguess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.mapper.GameRecordDetailMapper;
import com.drawguess.mapper.GameRecordMapper;
import com.drawguess.mapper.RoomMapper;
import com.drawguess.mapper.UserMapper;
import com.drawguess.model.dto.GameRecordVO;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.entity.GameRecord;
import com.drawguess.model.entity.GameRecordDetail;
import com.drawguess.model.entity.Room;
import com.drawguess.model.entity.User;
import com.drawguess.service.LeaderboardService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);
    private static final String LEADERBOARD_KEY = "leaderboard:total";
    private static final long LEADERBOARD_TTL_MINUTES = 10;

    private final UserMapper userMapper;
    private final GameRecordMapper gameRecordMapper;
    private final GameRecordDetailMapper gameRecordDetailMapper;
    private final RoomMapper roomMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public LeaderboardServiceImpl(UserMapper userMapper,
                                  GameRecordMapper gameRecordMapper,
                                  GameRecordDetailMapper gameRecordDetailMapper,
                                  RoomMapper roomMapper,
                                  StringRedisTemplate stringRedisTemplate,
                                  ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.gameRecordMapper = gameRecordMapper;
        this.gameRecordDetailMapper = gameRecordDetailMapper;
        this.roomMapper = roomMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<UserInfoVO> getLeaderboard() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(LEADERBOARD_KEY);
            if (cached != null) {
                log.debug("排行榜命中缓存");
                return objectMapper.readValue(cached, new TypeReference<List<UserInfoVO>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败，降级为数据库查询: {}", e.getMessage());
        }

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getTotalScore)
                        .last("LIMIT 100")
        );
        List<UserInfoVO> result = users.stream().map(UserInfoVO::fromEntity).collect(Collectors.toList());

        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(LEADERBOARD_KEY, json, LEADERBOARD_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis缓存写入失败: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public List<GameRecordVO> getUserGameRecords(Long userId) {
        List<GameRecord> records = gameRecordMapper.selectByUserId(userId);
        List<GameRecordVO> result = new ArrayList<>();

        for (GameRecord record : records) {
            GameRecordVO vo = new GameRecordVO();
            vo.setId(record.getId());
            vo.setRoomId(record.getRoomId());
            vo.setPlayedAt(record.getPlayedAt());

            Room room = roomMapper.selectById(record.getRoomId());
            if (room != null) {
                vo.setRoomCode(room.getRoomId());
            }

            List<GameRecordDetail> details = gameRecordDetailMapper.selectList(
                    new LambdaQueryWrapper<GameRecordDetail>()
                            .eq(GameRecordDetail::getRecordId, record.getId())
            );
            List<GameRecordVO.DetailVO> detailVOs = new ArrayList<>();
            for (GameRecordDetail d : details) {
                GameRecordVO.DetailVO detailVO = new GameRecordVO.DetailVO();
                detailVO.setUserId(d.getUserId());
                detailVO.setScore(d.getScore());
                detailVO.setWord(d.getWord());
                User u = userMapper.selectById(d.getUserId());
                if (u != null) {
                    detailVO.setNickname(u.getNickname());
                }
                detailVOs.add(detailVO);
            }
            vo.setDetails(detailVOs);
            result.add(vo);
        }

        return result;
    }

    @Override
    public void invalidateLeaderboardCache() {
        try {
            stringRedisTemplate.delete(LEADERBOARD_KEY);
            log.debug("排行榜缓存已清除");
        } catch (Exception e) {
            log.warn("清除排行榜缓存失败: {}", e.getMessage());
        }
    }
}
