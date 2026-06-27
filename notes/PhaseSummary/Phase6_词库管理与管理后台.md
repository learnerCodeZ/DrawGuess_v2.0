# 阶段六：词库管理 + 管理后台 — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段六

---

## 一、本阶段完成的工作

### 1. 词库管理完善

#### WordService 扩展

| 方法 | 说明 |
|------|------|
| `getAllWords()` | 返回 `WordVO` 列表（原来返回原始 `Word` 实体） |
| `getWordCount()` | 新增：词库总数 |
| `addWord()` | 增加 trim + 空字符串校验 |
| `batchAddWords()` | 新增：批量添加，自动跳过重复词 |
| `deleteWord()` | 不变：默认词不可删 |
| `getRandomWord()` | 不变：随机抽词 |

#### WordVO

新增 VO 替代原始实体返回，确保前端只看到必要字段：
```json
{ "id": 1, "word": "苹果", "isDefault": true, "createdAt": "..." }
```

#### 初始词库

| 类别 | 词语 | 是否默认 |
|------|------|---------|
| 默认词（不可删） | 苹果、大象、太阳、飞机、钢琴 | ✅ |
| 扩展词（可删） | 猫咪、月亮、汽车、蛋糕、足球等 20 个 | ❌ |

初始词库通过两个途径保证：
- `init.sql`（Docker 首次启动时执行）
- `DataInitConfig`（应用启动时，如果词库为空则自动填充）

### 2. 管理后台增强

#### 新增 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/words/count | 词库总数 |
| POST | /api/admin/words/batch | 批量添加词语 |
| POST | /api/admin/rooms/{roomId}/resume | 恢复暂停的房间 |
| GET | /api/admin/stats | 仪表盘统计数据 |
| GET | /api/admin/stats/online | 在线用户统计 |

#### 完整管理员 API 清单

| 模块 | 方法 | 路径 |
|------|------|------|
| 用户 | GET | /api/admin/users |
| 用户 | GET | /api/admin/users/pending |
| 用户 | GET | /api/admin/users/rejected |
| 用户 | POST | /api/admin/approve |
| 用户 | POST | /api/admin/reject |
| 用户 | DELETE | /api/admin/users/{userId} |
| 词库 | GET | /api/admin/words |
| 词库 | GET | /api/admin/words/count |
| 词库 | POST | /api/admin/words |
| 词库 | POST | /api/admin/words/batch |
| 词库 | DELETE | /api/admin/words/{wordId} |
| 房间 | GET | /api/admin/rooms |
| 房间 | POST | /api/admin/rooms/{roomId}/pause |
| 房间 | POST | /api/admin/rooms/{roomId}/resume |
| 房间 | POST | /api/admin/rooms/{roomId}/dismiss |
| 统计 | GET | /api/admin/stats |
| 统计 | GET | /api/admin/stats/online |

### 3. 房间恢复功能

暂停 → 恢复的完整流程：

```
管理员暂停: POST /api/admin/rooms/{roomId}/pause
  → room.state = PAUSED
  → 广播: room_paused

管理员恢复: POST /api/admin/rooms/{roomId}/resume
  → room.state = WAITING（恢复到等待状态，不是 PLAYING）
  → 校验: 只能从 PAUSED 恢复
  → 广播: room_resumed
```

### 4. 仪表盘统计

`GET /api/admin/stats` 返回：

```json
{
  "totalUsers": 50,
  "pendingUsers": 3,
  "approvedUsers": 45,
  "activeRooms": 8,
  "playingGames": 5,
  "totalWords": 25,
  "onlineUsers": 12
}
```

`GET /api/admin/stats/online` 返回：

```json
{
  "onlineCount": 12,
  "onlineUserIds": [1, 2, 3, 5, 8, ...]
}
```

### 5. AdminService 增强

注入了 `RoomMapper`、`WordMapper`、`GameService`、`OnlineUserService`，实现跨模块统计查询。

---

## 二、新增/修改文件清单

### 新增文件 (1)
- `model/dto/WordVO.java` — 词库 VO

### 修改文件 (7)
- `service/WordService.java` — 新增 getWordCount、batchAddWords，返回 WordVO
- `service/impl/WordServiceImpl.java` — 完整重写
- `service/AdminService.java` — 新增 getDashboardStats
- `service/impl/AdminServiceImpl.java` — 注入新依赖，实现统计
- `service/RoomService.java` — 新增 resumeRoom
- `service/impl/RoomServiceImpl.java` — 实现 resumeRoom
- `controller/AdminController.java` — 大幅增强（17 个端点）
- `config/DataInitConfig.java` — 增加词库初始化
- `docker/init.sql` — 扩展词库数据

---

## 三、核心架构决策

### 3.1 词库初始化：SQL vs 代码

**决策**：双保险——SQL 脚本 + Java 代码。

- `init.sql`：Docker Compose 首次启动时执行，包含建表 + 初始数据
- `DataInitConfig`：应用启动时检查，词库为空则填充

**原因**：
- Docker 场景下 init.sql 更可靠（只执行一次）
- 本地开发可能不用 Docker，Java 代码作为兜底
- 两者兼容：SQL 的 `INSERT IGNORE` 和 Java 的 `getWordCount() == 0` 检查避免重复

### 3.2 暂停恢复到 WAITING 而非 PLAYING

**决策**：暂停恢复后状态为 `WAITING`，不是 `PLAYING`。

**原因**：
- 暂停通常是管理员干预，游戏状态应重置
- 恢复后需要房主重新开始游戏，而不是自动继续
- 游戏进行中的状态（GameContext）已丢失，无法从 PLAYING 无缝恢复

### 3.3 统计查询跨多表

**决策**：直接在 AdminServiceImpl 中注入多个 Mapper 查询，不通过 Service 层。

**原因**：
- 统计查询是只读聚合，不需要业务逻辑
- 如果走 Service 层，需要给每个 Service 增加统计方法，过度设计
- Mapper 的 `selectCount()` 已经足够高效

---

## 四、PRD 功能对照（3.6 管理员功能）

| PRD 功能 | 实现状态 | 说明 |
|----------|---------|------|
| 用户审核：查看/通过/拒绝 | ✅ | pending + rejected 列表 + approve/reject |
| 用户管理：查看所有用户 | ✅ | getAllUsers |
| 用户管理：注销用户 | ✅ | deleteUser |
| 房间管理：查看在线房间 | ✅ | getOnlineRooms |
| 房间管理：暂停房间 | ✅ | pauseRoom |
| 房间管理：解散房间 | ✅ | dismissRoom |
| 词库管理：查看词语 | ✅ | getWords + getWordCount |
| 词库管理：新增词语 | ✅ | addWord + batchAddWords |
| 词库管理：删除词语 | ✅ | deleteWord（默认词不可删） |

---

## 五、待后续阶段完善

| 项目 | 计划阶段 |
|------|---------|
| 管理员后台前端页面 | 阶段七 |
| 管理员操作审计日志 | 如需可增加 AOP 切面 |
| 批量审核用户 | 如需可增加 |
