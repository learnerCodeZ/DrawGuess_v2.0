# 阶段四：房间系统 + WebSocket — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段四

---

## 一、本阶段完成的工作

### 1. WebSocket 认证拦截器

**文件**：`WebSocketAuthInterceptor.java`

在 STOMP 握手阶段（HTTP 升级请求）验证 JWT：

```
客户端连接: ws://host/ws?token=xxx
    → HandshakeInterceptor.beforeHandshake()
        → 从 URL 参数或 Authorization Header 提取 token
        → JwtTokenProvider.validateToken() 验证
        → 验证通过: 将 User 对象存入 session attributes
        → 验证失败: 拒绝握手（返回 false）
```

**为什么握手阶段验证**：
- STOMP 协议在 HTTP 升级后运行，无法再携带 HTTP Header
- 在握手时验证可确保所有后续 STOMP 消息都来自已认证用户
- User 对象存入 session，后续 `@MessageMapping` 方法直接使用

### 2. GameWebSocketHandler（STOMP 消息处理）

**文件**：`GameWebSocketHandler.java`

使用 Spring STOMP 的 `@MessageMapping` 注解处理客户端消息：

| STOMP 路由 | 事件 | 说明 |
|-----------|------|------|
| `/app/join_room` | join_room | 加入房间（委托 RoomService，内置广播） |
| `/app/leave_room` | leave_room | 离开房间（委托 RoomService，内置广播） |
| `/app/draw` | draw | 绘制数据转发（直接广播） |
| `/app/submit_answer` | submit_answer | 提交答案（阶段五完善逻辑） |
| `/app/start_game` | start_game | 开始游戏（阶段五完善逻辑） |

**广播路径设计**：

| 路径 | 用途 |
|------|------|
| `/topic/room/{roomId}` | 房间内广播（所有人收到） |
| `/user/{sessionId}/queue/errors` | 点对点错误消息 |

### 3. WsMessage 消息封装

**文件**：`WsMessage.java`

所有 WebSocket 消息统一格式：

```json
{
  "event": "user_joined",
  "data": { "userId": 1, "nickname": "张三" },
  "timestamp": 1700000000000
}
```

**事件列表**：

| 事件 | 触发方 | 说明 |
|------|-------|------|
| `room_created` | RoomService | 房间创建 |
| `user_joined` | RoomService | 用户加入 |
| `user_left` | RoomService | 用户离开 |
| `room_dismissed` | RoomService | 房间解散（房主/管理员） |
| `room_paused` | RoomService | 房间暂停（管理员） |
| `room_state` | RoomService | 房间完整状态同步 |
| `draw_data` | GameWebSocketHandler | 绘制数据 |
| `answer_result` | GameWebSocketHandler | 答题结果 |
| `game_start` | GameWebSocketHandler | 游戏开始 |
| `error` | GameWebSocketHandler | 错误消息 |

### 4. RoomService 集成 WebSocket 广播

**关键改动**：RoomService 注入 `SimpMessagingTemplate`，在房间操作后自动广播。

**好处**：
- REST API 和 WebSocket 都能触发房间操作，且广播行为一致
- 广播逻辑集中在 Service 层，Controller 和 Handler 无需关心
- 避免了双重广播问题（Handler 不再单独广播 join/leave）

**修复的问题**：
- 房主离开房间：原逻辑只删除成员记录，现在自动解散房间
- 普通成员离开：增加 `NOT_IN_ROOM` 校验

### 5. 用户在线状态

**文件**：`OnlineUserService.java` + `OnlineUserServiceImpl.java`

使用 Redis Set 维护在线用户列表：

| 操作 | Redis 命令 |
|------|-----------|
| 用户上线 | `SADD online:users {userId}` |
| 用户下线 | `SREM online:users {userId}` |
| 查询在线 | `SISMEMBER online:users {userId}` |
| 在线列表 | `SMEMBERS online:users` |
| 在线人数 | `SCARD online:users` |

**触发时机**：`WebSocketEventListener` 监听 Spring WebSocket 事件：
- `SessionConnectedEvent` → 用户上线
- `SessionDisconnectEvent` → 用户下线

**集成点**：
- 好友列表：`FriendServiceImpl.getFriendList()` 返回的 `UserInfoVO` 带 `online` 字段
- `UserInfoVO` 新增 `online` 字段和 `setOnlineStatus()` 方法

### 6. WebSocket 事件监听器

**文件**：`WebSocketEventListener.java`

监听 STOMP 连接/断开事件，自动维护在线状态。

---

## 二、新增/修改文件清单

### 新增文件 (5)
- `websocket/WebSocketAuthInterceptor.java` — STOMP 握手认证
- `websocket/GameWebSocketHandler.java` — STOMP 消息处理
- `websocket/WsMessage.java` — 消息封装
- `websocket/WebSocketEventListener.java` — 连接/断开监听
- `service/OnlineUserService.java` + `impl` — 在线状态服务

### 修改文件 (5)
- `config/WebSocketConfig.java` — 注入认证拦截器
- `service/RoomService.java` — 新增 getMemberUserIds
- `service/impl/RoomServiceImpl.java` — 集成广播 + 修复房主离开
- `service/impl/FriendServiceImpl.java` — 好友列表带在线状态
- `model/dto/UserInfoVO.java` — 新增 online 字段

---

## 三、核心架构决策

### 3.1 广播职责归属：Service vs Handler

**决策**：房间操作（join/leave/dissolve）的广播由 RoomService 负责，GameWebSocketHandler 只做消息路由。

**原因**：
- REST API 和 WebSocket 都能触发房间操作
- 广播逻辑应与业务逻辑同层，确保一致性
- Handler 层保持轻量，只做 STOMP 消息到 Service 方法的映射

### 3.2 WebSocket 认证：握手 vs 消息级

**决策**：握手阶段验证（`HandshakeInterceptor`），消息级不再验证。

**原因**：
- 握手验证后 User 存入 session，所有消息自动携带
- 消息级验证需要 ChannelInterceptor，增加复杂度
- 当前场景（局域网游戏）握手验证足够安全
- 后续如需更细粒度控制，可在 `configureClientInboundChannel` 中添加

### 3.3 在线状态：Redis Set vs Redis Hash

**决策**：Redis Set（`SADD/SREM`）

**原因**：
- 只需存储 userId，不需要附加信息（如连接时间）
- Set 操作 O(1)，适合频繁增删
- `SISMEMBER` 查单个用户在线状态非常快
- 如需附加信息，可迁移到 Hash（`HSET online:users {userId} {connectTime}`）

### 3.4 STOMP vs 原生 WebSocket

**决策**：STOMP over WebSocket（保持阶段一的选择）

**好处在本阶段体现**：
- `@MessageMapping` 注解简化了消息路由
- `SimpMessagingTemplate` 提供了便捷的广播 API
- `/topic/room/{roomId}` 天然实现了房间订阅语义
- SockJS 降级支持（`withSockJS()`）

---

## 四、WebSocket 事件对照（PRD 5.2 节）

| PRD 事件 | 实现状态 | 说明 |
|----------|---------|------|
| C→S connect | ✅ | SockJS 自动处理 |
| C→S auth | ✅ | 握手阶段 token 参数 |
| C→S join_room | ✅ | `/app/join_room` |
| C→S draw | ✅ | `/app/draw` |
| C→S submit_answer | ✅ | `/app/submit_answer`（逻辑待阶段五） |
| C→S start_game | ✅ | `/app/start_game`（逻辑待阶段五） |
| C→S end_round | ❌ | 阶段五实现 |
| S→C auth_ok/auth_error | ✅ | 握手成功/失败 |
| S→C user_joined/user_left | ✅ | RoomService 广播 |
| S→C room_state | ✅ | 每次操作后同步 |
| S→C round_start | ❌ | 阶段五实现 |
| S→C draw_data | ✅ | Handler 转发 |
| S→C answer_correct | ❌ | 阶段五实现 |
| S→C answer_result | ✅ | 阶段五完善逻辑 |
| S→C game_end | ❌ | 阶段五实现 |
| S→C room_paused/room_dismissed | ✅ | RoomService 广播 |

---

## 五、待阶段五完善

| 项目 | 说明 |
|------|------|
| 游戏状态机 | waiting → playing → ended 完整流转 |
| 画家分配与回合流转 | 轮流当画家、30秒倒计时 |
| 答题正确判断 | submit_answer 中比较答案 |
| round_start/game_end 事件 | 游戏核心流程 |
| end_round 事件 | 回合结束 |
| answer_correct 事件 | 猜对通知 |
