package com.drawguess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.mapper.GameRecordDetailMapper;
import com.drawguess.mapper.GameRecordMapper;
import com.drawguess.mapper.RoomMemberMapper;
import com.drawguess.mapper.UserMapper;
import com.drawguess.model.dto.GameResultVO;
import com.drawguess.model.dto.RoundInfo;
import com.drawguess.model.entity.GameRecord;
import com.drawguess.model.entity.GameRecordDetail;
import com.drawguess.model.entity.Room;
import com.drawguess.model.entity.RoomMember;
import com.drawguess.model.entity.User;
import com.drawguess.model.enums.RoomState;
import com.drawguess.service.GameContext;
import com.drawguess.service.GameService;
import com.drawguess.service.LeaderboardService;
import com.drawguess.service.RoomService;
import com.drawguess.service.WordService;
import com.drawguess.websocket.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameServiceImpl implements GameService {

    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);
    private static final int MIN_PLAYERS = 3;
    private static final int ROUND_TIME_SECONDS = 30;
    private static final int ROUND_BREAK_SECONDS = 10;
    private static final int SCORE_FOR_CORRECT = 1;

    /** 房间ID → 游戏上下文（内存维护） */
    private final ConcurrentHashMap<String, GameContext> gameContexts = new ConcurrentHashMap<>();

    private final RoomService roomService;
    private final WordService wordService;
    private final LeaderboardService leaderboardService;
    private final RoomMemberMapper roomMemberMapper;
    private final UserMapper userMapper;
    private final GameRecordMapper gameRecordMapper;
    private final GameRecordDetailMapper gameRecordDetailMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public GameServiceImpl(RoomService roomService, WordService wordService,
                           LeaderboardService leaderboardService,
                           RoomMemberMapper roomMemberMapper, UserMapper userMapper,
                           GameRecordMapper gameRecordMapper,
                           GameRecordDetailMapper gameRecordDetailMapper,
                           SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.wordService = wordService;
        this.leaderboardService = leaderboardService;
        this.roomMemberMapper = roomMemberMapper;
        this.userMapper = userMapper;
        this.gameRecordMapper = gameRecordMapper;
        this.gameRecordDetailMapper = gameRecordDetailMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void startGame(String roomId, Long userId) {
        Room room = roomService.getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        if (!room.getCreatorId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_ROOM_CREATOR);
        }
        if (room.getState() != RoomState.WAITING) {
            throw new BusinessException(ResultCode.ROOM_ALREADY_PLAYING);
        }

        // 检查人数
        List<RoomMember> members = getRoomMembers(room.getId());
        if (members.size() < MIN_PLAYERS) {
            throw new BusinessException(ResultCode.NOT_ENOUGH_PLAYERS);
        }

        // 初始化游戏上下文
        GameContext ctx = new GameContext();
        ctx.setRoomId(roomId);
        ctx.setState(RoomState.PLAYING);

        // 随机分配画家顺序
        List<Long> order = members.stream().map(RoomMember::getUserId).toList();
        List<Long> shuffled = new ArrayList<>(order);
        Collections.shuffle(shuffled);
        ctx.setPainterOrder(shuffled);

        gameContexts.put(roomId, ctx);

        // 更新房间状态
        room.setState(RoomState.PLAYING);
        roomService.updateRoom(room);

        // 广播游戏开始
        broadcast(roomId, "game_start", Map.of(
                "roomId", roomId,
                "totalRounds", ctx.getTotalRounds(),
                "painterOrder", shuffled
        ));

        // 自动开始第一回合
        startRound(roomId);

        log.info("游戏开始: roomId={}, rounds={}, players={}", roomId, ctx.getTotalRounds(), members.size());
    }

    @Override
    public RoundInfo startRound(String roomId) {
        GameContext ctx = gameContexts.get(roomId);
        if (ctx == null || !ctx.isPlaying()) {
            throw new BusinessException(ResultCode.GAME_NOT_IN_PROGRESS);
        }

        // 获取当前画家
        Long painterId = ctx.getPainterOrder().get(ctx.getCurrentRoundIndex());
        ctx.setCurrentPainterId(painterId);

        // 随机抽词
        var word = wordService.getRandomWord();
        ctx.setCurrentWord(word.getWord());
        ctx.setAnsweredUserIds(new java.util.HashSet<>());
        ctx.setRoundTimeLeft(ROUND_TIME_SECONDS);

        // 构建回合信息（非画家不看到词）
        RoundInfo roundInfo = buildRoundInfo(ctx);

        // 广播回合开始（画家看到完整词，其他人看到字数）
        broadcast(roomId, "round_start", Map.of(
                "roundNumber", ctx.getCurrentRoundNumber(),
                "totalRounds", ctx.getTotalRounds(),
                "painterId", painterId,
                "painterNickname", getNickname(painterId),
                "word", ctx.getCurrentWord(),
                "wordLength", ctx.getCurrentWord().length(),
                "timeLeft", ROUND_TIME_SECONDS
        ));

        log.info("回合开始: roomId={}, round={}/{}, painterId={}, word={}",
                roomId, ctx.getCurrentRoundNumber(), ctx.getTotalRounds(), painterId, ctx.getCurrentWord());

        return roundInfo;
    }

    @Override
    public RoundInfo getCurrentRound(String roomId) {
        GameContext ctx = gameContexts.get(roomId);
        if (ctx == null) return null;
        return buildRoundInfo(ctx);
    }

    @Override
    public boolean submitAnswer(String roomId, Long userId, String answer) {
        GameContext ctx = gameContexts.get(roomId);
        if (ctx == null || !ctx.isPlaying()) {
            throw new BusinessException(ResultCode.GAME_NOT_IN_PROGRESS);
        }

        // 画家不能答题
        if (ctx.isPainter(userId)) {
            return false;
        }

        // 已答过
        if (ctx.hasAnswered(userId)) {
            throw new BusinessException(ResultCode.ALREADY_ANSWERED);
        }

        // 判断答案
        boolean correct = ctx.getCurrentWord().equals(answer);

        if (correct) {
            ctx.getAnsweredUserIds().add(userId);

            // 猜对者得分
            ctx.addScore(userId, SCORE_FOR_CORRECT);
            // 画家得分
            ctx.addScore(ctx.getCurrentPainterId(), SCORE_FOR_CORRECT);

            // 更新 room_member 的本局分数
            updateMemberScore(roomId, userId, SCORE_FOR_CORRECT);
            updateMemberScore(roomId, ctx.getCurrentPainterId(), SCORE_FOR_CORRECT);

            // 广播有人猜对
            broadcast(roomId, "answer_correct", Map.of(
                    "userId", userId,
                    "nickname", getNickname(userId),
                    "painterId", ctx.getCurrentPainterId(),
                    "word", ctx.getCurrentWord()
            ));

            log.info("答题正确: roomId={}, userId={}, word={}", roomId, userId, ctx.getCurrentWord());

            // 如果所有非画家都猜对了，提前结束回合
            List<RoomMember> members = getRoomMembers(roomService.getRoomByRoomId(roomId).getId());
            long nonPainterCount = members.stream()
                    .filter(m -> !m.getUserId().equals(ctx.getCurrentPainterId()))
                    .count();
            if (ctx.getAnsweredUserIds().size() >= nonPainterCount) {
                endRound(roomId);
            }
        } else {
            // 广播答题错误
            broadcast(roomId, "answer_result", Map.of(
                    "userId", userId,
                    "nickname", getNickname(userId),
                    "correct", false
            ));
        }

        return correct;
    }

    @Override
    public void endRound(String roomId) {
        GameContext ctx = gameContexts.get(roomId);
        if (ctx == null || !ctx.isPlaying()) {
            throw new BusinessException(ResultCode.GAME_NOT_IN_PROGRESS);
        }

        // 构建回合得分摘要（含昵称和总分）
        List<Map<String, Object>> roundSummary = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : ctx.getRoundScores().entrySet()) {
            User user = userMapper.selectById(entry.getKey());
            if (user == null) continue;
            roundSummary.add(Map.of(
                    "userId", entry.getKey(),
                    "nickname", user.getNickname(),
                    "totalScore", user.getTotalScore() + entry.getValue(),
                    "roundScore", entry.getValue()
            ));
        }
        // 按本局得分降序
        roundSummary.sort((a, b) -> (int) b.get("roundScore") - (int) a.get("roundScore"));

        // 广播回合结束
        broadcast(roomId, "round_end", Map.of(
                "roundNumber", ctx.getCurrentRoundNumber(),
                "word", ctx.getCurrentWord(),
                "painterNickname", getNickname(ctx.getCurrentPainterId()),
                "rankings", roundSummary
        ));

        log.info("回合结束: roomId={}, round={}/{}, word={}",
                roomId, ctx.getCurrentRoundNumber(), ctx.getTotalRounds(), ctx.getCurrentWord());

        // 判断是否最后一回合
        if (ctx.isLastRound()) {
            endGame(roomId);
        } else {
            ctx.advanceRound();
            // 10 秒间隔后自动开始下一回合
            broadcast(roomId, "round_break", Map.of(
                    "breakSeconds", ROUND_BREAK_SECONDS,
                    "nextRoundNumber", ctx.getCurrentRoundNumber(),
                    "totalRounds", ctx.getTotalRounds(),
                    "rankings", roundSummary
            ));
            new Thread(() -> {
                try {
                    Thread.sleep(ROUND_BREAK_SECONDS * 1000L);
                    startRound(roomId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @Override
    public void endGame(String roomId) {
        GameContext ctx = gameContexts.get(roomId);
        if (ctx == null) return;

        ctx.setState(RoomState.ENDED);

        // 更新房间状态
        Room room = roomService.getRoomByRoomId(roomId);
        if (room != null) {
            room.setState(RoomState.ENDED);
            roomService.updateRoom(room);
        }

        // 构建结算数据
        List<GameResultVO.PlayerScore> rankings = new ArrayList<>();
        Map<Long, Integer> scoreChanges = ctx.getRoundScores();
        for (Map.Entry<Long, Integer> entry : scoreChanges.entrySet()) {
            User user = userMapper.selectById(entry.getKey());
            if (user == null) continue;

            // 累加到用户总分
            user.setTotalScore(user.getTotalScore() + entry.getValue());
            userMapper.updateById(user);

            GameResultVO.PlayerScore ps = new GameResultVO.PlayerScore();
            ps.setUserId(user.getId());
            ps.setNickname(user.getNickname());
            ps.setGameScore(entry.getValue());
            ps.setTotalScore(user.getTotalScore());
            rankings.add(ps);
        }

        // 按本局得分降序排列
        rankings.sort((a, b) -> b.getGameScore() - a.getGameScore());

        // 保存游戏记录到数据库
        if (room != null) {
            GameRecord record = new GameRecord();
            record.setRoomId(room.getId());
            record.setPlayedAt(LocalDateTime.now());
            gameRecordMapper.insert(record);

            for (GameResultVO.PlayerScore ps : rankings) {
                GameRecordDetail detail = new GameRecordDetail();
                detail.setRecordId(record.getId());
                detail.setUserId(ps.getUserId());
                detail.setScore(ps.getGameScore());
                detail.setWord(ctx.getCurrentWord());
                gameRecordDetailMapper.insert(detail);
            }
        }

        // 广播游戏结束
        broadcast(roomId, "game_end", Map.of(
                "roomId", roomId,
                "rankings", rankings,
                "scoreChanges", scoreChanges
        ));

        // 清除排行榜缓存
        leaderboardService.invalidateLeaderboardCache();

        // 清理上下文
        gameContexts.remove(roomId);

        log.info("游戏结束: roomId={}, 玩家数={}", roomId, rankings.size());
    }

    @Override
    public GameContext getGameContext(String roomId) {
        return gameContexts.get(roomId);
    }

    @Override
    public boolean isGamePlaying(String roomId) {
        GameContext ctx = gameContexts.get(roomId);
        return ctx != null && ctx.isPlaying();
    }

    @Override
    public java.util.Set<String> getActiveRoomIds() {
        return gameContexts.keySet();
    }

    // ---- 内部辅助方法 ----

    private List<RoomMember> getRoomMembers(Long roomDbId) {
        return roomMemberMapper.selectList(
                new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getRoomId, roomDbId)
        );
    }

    private void updateMemberScore(String roomId, Long userId, int addScore) {
        Room room = roomService.getRoomByRoomId(roomId);
        if (room == null) return;
        RoomMember member = roomMemberMapper.selectOne(
                new LambdaQueryWrapper<RoomMember>()
                        .eq(RoomMember::getRoomId, room.getId())
                        .eq(RoomMember::getUserId, userId)
        );
        if (member != null) {
            member.setScore(member.getScore() + addScore);
            roomMemberMapper.updateById(member);
        }
    }

    private String getNickname(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getNickname() : "";
    }

    private RoundInfo buildRoundInfo(GameContext ctx) {
        RoundInfo info = new RoundInfo();
        info.setRoundNumber(ctx.getCurrentRoundNumber());
        info.setTotalRounds(ctx.getTotalRounds());
        info.setPainterId(ctx.getCurrentPainterId());
        info.setPainterNickname(getNickname(ctx.getCurrentPainterId()));
        info.setWord(ctx.getCurrentWord());
        info.setWordLength(ctx.getCurrentWord() != null ? ctx.getCurrentWord().length() : 0);
        info.setTimeLeft(ctx.getRoundTimeLeft());
        info.setAnsweredUserIds(new ArrayList<>(ctx.getAnsweredUserIds()));
        return info;
    }

    private void broadcast(String roomId, String event, Object data) {
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, WsMessage.event(event, data));
        } catch (Exception e) {
            log.warn("广播失败: roomId={}, event={}, error={}", roomId, event, e.getMessage());
        }
    }
}
