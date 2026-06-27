# 阶段五：游戏核心逻辑 — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段五

---

## 一、本阶段完成的工作

### 1. 游戏状态机

完整实现了游戏生命周期：

```
WAITING → startGame() → PLAYING
    → startRound() → [画家画画 + 其他人猜] → endRound()
        → 非最后回合? → startRound()（自动进入下一回合）
        → 最后回合? → endGame() → ENDED
```

关键节点：
- **开局条件**：≥ 3 人
- **画家分配**：随机洗牌，每人当一次画家
- **回合时间**：30 秒倒计时，到时自动结束
- **提前结束**：所有非画家都猜对后自动结束回合
- **终局结算**：累加到用户总分 + 保存游戏记录

### 2. GameContext（游戏状态上下文）

**文件**：`GameContext.java`

内存中维护每个房间的实时游戏状态，使用 `ConcurrentHashMap<roomId, GameContext>` 存储：

| 字段 | 类型 | 说明 |
|------|------|------|
| `roomId` | String | 房间号 |
| `state` | RoomState | 游戏状态 |
| `painterOrder` | List\<Long\> | 画家顺序（洗牌后） |
| `currentRoundIndex` | int | 当前回合索引 |
| `currentWord` | String | 当前词语 |
| `currentPainterId` | Long | 当前画家 |
| `answeredUserIds` | Set\<Long\> | 已答对的人 |
| `roundTimeLeft` | int | 剩余秒数 |
| `roundScores` | Map\<Long, Integer\> | 本局各人得分 |

### 3. GameServiceImpl 完整实现

| 方法 | 说明 |
|------|------|
| `startGame()` | 校验条件 → 洗牌分配画家 → 初始化上下文 → 开始第一回合 |
| `startRound()` | 取画家 → 抽词 → 广播回合开始 → 给画家私发词语 |
| `submitAnswer()` | 校验身份 → 比对答案 → 计分 → 判断是否全部猜对 |
| `endRound()` | 广播回合结果 → 判断是否最后回合 → 自动流转或结束 |
| `endGame()` | 累加总分 → 保存游戏记录 → 清除排行榜缓存 → 清理上下文 |
| `getActiveRoomIds()` | 返回所有活跃游戏房间（供定时器遍历） |

### 4. 30 秒倒计时定时器

**文件**：`GameTimerConfig.java`

使用 Spring `@Scheduled(fixedRate = 1000)` 每秒遍历所有活跃游戏：

```
每秒执行:
  遍历所有活跃房间 →
    timeLeft > 0? → timeLeft-- → 广播 tick 事件 →
      timeLeft == 0? → 自动调用 endRound()
```

广播事件：
```json
{ "event": "tick", "data": { "timeLeft": 25, "roundNumber": 3 }, "timestamp": ... }
```

### 5. WebSocket 事件完整覆盖

GameWebSocketHandler 现在处理 6 种 STOMP 消息：

| STOMP 路由 | 委托方法 | 说明 |
|-----------|---------|------|
| `/app/join_room` | RoomService | 加入房间 |
| `/app/leave_room` | RoomService | 离开房间 |
| `/app/draw` | 直接广播 | 绘制数据 |
| `/app/submit_answer` | GameService | 提交答案 |
| `/app/start_game` | GameService | 开始游戏 |
| `/app/end_round` | GameService | 结束回合 |

### 6. 新增 VO

| VO | 说明 |
|----|------|
| `RoundInfo` | 回合信息（轮次、画家、词语/字数、倒计时、已答对列表） |
| `GameResultVO` | 终局结算（排名、得分变化） |
| `GameResultVO.PlayerScore` | 单人得分（userId、昵称、本局得分、总分） |

### 7. REST API 新增

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/game/round?roomId= | 查询当前回合信息 |

---

## 二、游戏完整流程（从前端视角）

```
1. 房主点击"开始游戏"
   → WS: /app/start_game { roomId }
   → 广播: game_start { totalRounds, painterOrder }
   → 自动: round_start { painterId, wordLength, timeLeft }
   → 画家私发: round_word { word }

2. 画家画画
   → WS: /app/draw { roomId, data: { points, color, width } }
   → 广播: draw_data { userId, data }

3. 其他人猜词
   → WS: /app/submit_answer { roomId, answer }
   → 猜错: 广播 answer_result { correct: false }
   → 猜对: 广播 answer_correct { userId, word } + 计分

4. 倒计时每秒更新
   → 广播: tick { timeLeft, roundNumber }

5. 回合结束（时间到/全员猜对/画家主动结束）
   → 广播: round_end { word, scores }
   → 非最后回合: 自动开始下一轮 (回到步骤1后半段)
   → 最后回合: endGame

6. 游戏结束
   → 广播: game_end { rankings, scoreChanges }
   → 保存数据库 + 清除排行榜缓存
```

---

## 三、新增/修改文件清单

### 新增文件 (3)
- `service/GameContext.java` — 游戏状态上下文
- `model/dto/RoundInfo.java` — 回合信息 VO
- `model/dto/GameResultVO.java` — 终局结算 VO
- `config/GameTimerConfig.java` — 倒计时定时器

### 修改文件 (4)
- `service/GameService.java` — 扩展接口（7个方法）
- `service/impl/GameServiceImpl.java` — 完整重写
- `websocket/GameWebSocketHandler.java` — 集成 GameService
- `controller/GameController.java` — 新增 round 查询，answer 返回 correct

---

## 四、核心架构决策

### 4.1 游戏状态：内存 vs Redis

**决策**：游戏实时状态（`GameContext`）存内存，最终结果持久化到数据库。

**原因**：
- 游戏状态变更极频繁（每秒 tick、每笔画），Redis 延迟不可接受
- `ConcurrentHashMap` 提供线程安全且 O(1) 的访问
- 游戏结束后立即持久化，服务器重启最多丢失一局未完成的游戏
- 局域网场景下单节点部署，不存在分布式一致性问题

**权衡**：如果需要多节点部署，可改用 Redis + 发布订阅，但当前场景不需要。

### 4.2 词语私发：点对点 vs 广播过滤

**决策**：词语通过 `/user/{painterId}/queue/word` 私发给画家，非画家只收到字数。

**原因**：
- 前端过滤不可靠（客户端可修改 JS 看到词语）
- STOMP 的 user destination 天然支持点对点
- 非画家只能从 `round_start` 事件获取 `wordLength`

### 4.3 定时器：Spring Scheduled vs ScheduledExecutorService

**决策**：Spring `@Scheduled`，单线程轮询所有活跃游戏。

**原因**：
- 活跃游戏房间数有限（目标 ≥ 50），每秒遍历开销极小
- 单线程保证 GameContext 不会被并发修改（定时器和消息处理在同一线程池可能冲突，但 Spring 默认单线程调度避免了竞争）
- 如果性能不足，可改用 `ScheduledExecutorService` 按房间分配线程

---

## 五、PRD WebSocket 事件完整覆盖

| PRD 事件 | 实现状态 | 说明 |
|----------|---------|------|
| C→S connect | ✅ | SockJS |
| C→S auth | ✅ | 握手 token |
| C→S join_room | ✅ | `/app/join_room` |
| C→S draw | ✅ | `/app/draw` |
| C→S submit_answer | ✅ | `/app/submit_answer`（含答题判断+计分） |
| C→S start_game | ✅ | `/app/start_game`（含完整初始化） |
| C→S end_round | ✅ | `/app/end_round` |
| S→C auth_ok/auth_error | ✅ | 握手阶段 |
| S→C user_joined/user_left | ✅ | RoomService 广播 |
| S→C room_state | ✅ | RoomService 广播 |
| S→C round_start | ✅ | GameService.startRound() |
| S→C draw_data | ✅ | Handler 转发 |
| S→C answer_correct | ✅ | GameService.submitAnswer() |
| S→C answer_result | ✅ | GameService.submitAnswer() |
| S→C game_end | ✅ | GameService.endGame() |
| S→C room_paused/room_dismissed | ✅ | RoomService 广播 |
| S→C tick (新增) | ✅ | 定时器每秒广播 |

**16/16 PRD 事件全部实现**，另新增 1 个 tick 事件。

---

## 六、待后续阶段完善

| 项目 | 计划阶段 |
|------|---------|
| 画家中途断线处理 | 阶段七（前端联调时） |
| 管理员暂停/恢复游戏 | 阶段六 |
| 画布撤销功能（前端为主） | 阶段七 |
| 并发安全（多线程消息处理） | 如需多节点部署时优化 |
