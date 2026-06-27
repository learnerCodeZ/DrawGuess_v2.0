# 阶段三：好友系统 + 排行榜 — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段三

---

## 一、本阶段完成的工作

### 1. 好友系统完善

阶段一的 FriendController/FriendService 是骨架，本阶段将其打磨为可用状态：

#### 新增接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/friends | 获取好友列表（阶段一遗漏） |

#### 修复问题

| 问题 | 修复 |
|------|------|
| 好友请求列表返回原始 `Friend` 实体 | 改为返回 `FriendRequestVO`（含发送者昵称、手机号） |
| `acceptFriendRequest`/`rejectFriendRequest` 不校验状态 | 增加状态必须为 `PENDING` 的校验 |

#### API 完整清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/friends/search?q= | 搜索用户 |
| POST | /api/friends/request | 发送好友请求 |
| GET | /api/friends/requests | 查看收到的好友请求 |
| POST | /api/friends/accept | 同意好友请求 |
| POST | /api/friends/reject | 拒绝好友请求 |
| GET | /api/friends | 好友列表 |
| DELETE | /api/friends/{friendId} | 删除好友 |

### 2. 排行榜 + Redis 缓存

#### Redis 缓存方案

| 项目 | 方案 |
|------|------|
| 缓存键 | `leaderboard:total` |
| 过期时间 | 10 分钟 |
| 序列化 | `StringRedisTemplate` + Jackson 手动序列化 |
| 降级策略 | Redis 不可用时自动降级为数据库查询 |
| 缓存失效 | `invalidateLeaderboardCache()` 方法，供 GameService 分数变化时调用 |

**为什么用 `StringRedisTemplate` 而非 `RedisTemplate<String, Object>`**：
- `GenericJackson2JsonRedisSerializer` 会在 JSON 中写入 `@class` 类型信息
- 反序列化 `List<UserInfoVO>` 等泛型集合时容易出错
- `StringRedisTemplate` + 手动 Jackson 更可控，且便于调试（Redis 中存储可读 JSON）

### 3. 个人游戏记录查询

阶段一留了 TODO，本阶段完整实现：

**查询路径**：
```
用户ID → room_member(找参与过的房间) → game_record(找游戏记录) → game_record_detail(找每局详情)
```

**SQL 实现**：
```sql
SELECT DISTINCT gr.* FROM game_record gr
INNER JOIN room_member rm ON gr.room_id = rm.room_id
WHERE rm.user_id = #{userId}
ORDER BY gr.played_at DESC
```

**返回结构**：
```json
{
  "id": 1,
  "roomId": 100,
  "roomCode": "123456",
  "playedAt": "2024-01-01 12:00:00",
  "details": [
    { "userId": 1, "nickname": "张三", "score": 3, "word": "苹果" },
    { "userId": 2, "nickname": "李四", "score": 2, "word": "苹果" }
  ]
}
```

### 4. 新增 Mapper

| Mapper | 说明 |
|--------|------|
| `GameRecordDetailMapper` | 阶段一遗漏，本阶段补全 |
| `GameRecordMapper.selectByUserId()` | 自定义 SQL，关联 room_member 查询 |

---

## 二、新增/修改文件清单

### 新增文件 (3)
- `model/dto/FriendRequestVO.java` — 好友请求 VO
- `model/dto/GameRecordVO.java` — 游戏记录 VO（含内嵌 DetailVO）
- `mapper/GameRecordDetailMapper.java` — 游戏记录详情 Mapper

### 修改文件 (7)
- `controller/FriendController.java` — 新增好友列表接口，返回类型改为 VO
- `controller/LeaderboardController.java` — 返回类型改为 GameRecordVO
- `service/FriendService.java` — getFriendRequests 返回 FriendRequestVO
- `service/LeaderboardService.java` — 返回 GameRecordVO，新增缓存失效方法
- `service/impl/FriendServiceImpl.java` — 请求列表返回 VO，增加状态校验
- `service/impl/LeaderboardServiceImpl.java` — Redis 缓存 + 游戏记录关联查询
- `mapper/GameRecordMapper.java` — 新增 selectByUserId 自定义 SQL

---

## 三、核心架构决策

### 3.1 好友关系的双向查询

好友关系在数据库中只存一行（userId=1, friendId=2），但查询时需要双向查找。

**方案**：每次查询时用 `.and(w -> w.eq(userId).or().eq(friendId))` 构建双向条件。

**权衡**：
- 存一行：写入简单，但查询略复杂，N+1 问题（每条好友关系需查一次用户）
- 存两行：查询简单（只查 userId），但写入需双写，删除需双删

当前选存一行，因为好友列表规模小（通常几十人），N+1 影响可忽略。

### 3.2 排行榜缓存策略

**方案**：Cache-Aside（旁路缓存）+ TTL 过期

```
读：Redis 有 → 返回缓存
     Redis 无 → 查库 → 写缓存(TTL=10min) → 返回
写：分数变化 → 删除缓存 → 下次读时重建
```

不选 Write-Through 的原因：分数更新频率低（每局游戏结束才更新），没必要在写路径加缓存逻辑。

### 3.3 VO 设计：内嵌类 vs 独立类

`GameRecordVO.DetailVO` 使用静态内部类，而非独立文件。

**原因**：
- DetailVO 只在 GameRecordVO 中使用，不会独立出现
- 减少文件数，内聚性更好
- 当结构简单（3-4 个字段）时，内部类更清晰

---

## 四、API 对照（PRD 5.1 节）

| PRD 要求 | 实现状态 |
|----------|---------|
| GET /api/friends/search?q= | ✅ 已实现 |
| POST /api/friends/request | ✅ 已实现 |
| GET /api/friends/requests | ✅ 已实现（增强为 FriendRequestVO） |
| POST /api/friends/accept | ✅ 已实现 |
| POST /api/friends/reject | ✅ 已实现 |
| DELETE /api/friends/{friendId} | ✅ 已实现 |
| GET /api/leaderboard | ✅ 已实现（含 Redis 缓存） |
| GET /api/user/games | ✅ 已实现（含关联查询） |

---

## 五、待后续阶段完善

| 项目 | 计划阶段 |
|------|---------|
| 用户在线状态（Redis Set） | 阶段四（WebSocket 连接时维护） |
| 好友在线状态标记 | 阶段四（依赖在线状态） |
| 排行榜分页 | 如有需要再增加 |
| GameService 计分时调用 invalidateLeaderboardCache | 阶段五 |
