# 你画我猜 v2.0 — 产品需求文档 (PRD)

> 版本: v2.0  
> 状态: 规划中  
> 技术栈: Java Spring Boot + MyBatis + MySQL + WebSocket + Redis

---

## 1. 产品概述

### 1.1 产品背景

"你画我猜 v2.0" 是原 Python Flask 版 (DrawGuessingWeb_vibecoding) 的**全面 Java 技术栈重写升级版**。原版为一款多人实时协作的"你画我猜"Web 游戏，支持房间创建、好友邀请、实时作画与竞猜，配有用户管理、管理员审核及词库管理。

v2.0 沿用原版全部功能，同时在性能、安全性、可维护性、部署体验上全面升级，目标是打造一个**可上线的生产级实时游戏系统**，同时作为 Java 后端面试的实战项目。

### 1.2 产品目标

- 完整实现"你画我猜"游戏玩法的所有功能
- 采用 Java 主流技术栈重构，遵循企业级开发规范
- 提供更好的性能、安全性和部署体验
- 面向局域网内的轻量级聚会游戏场景

### 1.3 目标用户

- **普通用户**：参加游戏、作画猜词、社交互动
- **房主**：创建和管理游戏房间
- **管理员**：审核用户、管理词库、管理房间

### 1.4 核心指标

| 指标 | 目标值 |
|------|--------|
| 页面加载时间 | ≤ 2 秒 |
| 实时通信延迟 | ≤ 100ms |
| 同时在线用户 | ≥ 200 人 |
| 同时进行游戏房间 | ≥ 50 个 |

---

## 2. 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.x | 主框架 |
| ORM | MyBatis-Plus | 数据库操作 |
| 构建工具 | Maven | 项目构建 |
| 数据库 | MySQL 8.x | 持久化存储 |
| 缓存 | Redis 7.x | Session 管理、缓存加速 |
| WebSocket | Spring WebSocket / STOMP | 实时画布同步、游戏状态流转 |
| 认证授权 | Spring Security + JWT | 登录认证、接口保护 |
| 密码加密 | BCrypt | 安全密码存储 |
| API 文档 | SpringDoc OpenAPI (Swagger) | 接口文档自动生成 |
| 日志 | SLF4J + Logback | 规范日志 |
| 前端 | 原生 HTML + CSS + JS (可后期升级 Vue 3) | 保留原版 UI |
| 部署 | Docker + docker-compose | 容器化部署 |
| 测试 | JUnit 5 + Mockito | 单元测试 |

### 2.1 与原版技术栈对比

| 模块 | 原版 (Python) | v2.0 (Java) | 提升点 |
|------|--------------|-------------|--------|
| 后端语言 | Python (Flask) | Java (Spring Boot) | 匹配岗位要求 |
| 数据库 | JSON 文件 | MySQL + MyBatis | 支持复杂查询和事务 |
| 缓存 | 无 | Redis | 提升并发性能 |
| 认证 | 内存 dict + 明文密码 | JWT + BCrypt | 无状态、安全 |
| 实时通信 | Flask-SocketIO | Spring WebSocket (STOMP) | 标准化协议 |
| 代码结构 | 单文件 + 简单模块 | Controller/Service/DAO 分层 | 企业级规范 |
| 日志 | print() | SLF4J + Logback | 可维护性 |
| 部署 | 手动运行 | Docker 一键部署 | 标准化交付 |

---

## 3. 功能需求

### 3.1 用户系统

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 注册 | 昵称 + 手机号 + 密码，提交后需管理员审核 | P0 |
| 登录 | 手机号 + 密码，返回 JWT Token | P0 |
| 修改昵称 | 登录后在个人中心修改 | P0 |
| 修改密码 | 需验证旧密码 | P0 |
| 注销账户 | 自助注销，手机号不可修改 | P1 |
| 超级管理员强制改密 | 首次登录超级管理员需强制修改默认密码 | P0 |
| 记住登录状态 | Token 有效期 + Redis 缓存 | P2 |

### 3.2 好友系统

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 搜索用户 | 按昵称或手机号搜索（仅已审核用户） | P0 |
| 发送好友请求 | 向其他用户发送请求 | P0 |
| 处理好友请求 | 同意/拒绝 | P0 |
| 好友列表 | 查看好友及其在线状态 | P0 |
| 删除好友 | 单向删除 | P1 |

### 3.3 房间系统

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 创建房间 | 房主创建，自动生成 6 位房间号 | P0 |
| 加入房间 | 通过房间号加入 | P0 |
| 查看房间成员 | 显示所有在线成员 | P0 |
| 离开房间 | 成员主动退出 | P0 |
| 解散房间 | 房主解散 | P1 |
| 管理员暂停/解散 | 管理员可暂停或解散违规房间 | P0 |

### 3.4 游戏核心

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 开局条件 | 房内 ≥ 3 人能开始 | P0 |
| 画家分配 | 按随机顺序轮流，每人当一次 | P0 |
| 作画阶段 | 30 秒倒计时，鼠标/触摸绘制 | P0 |
| 画布工具 | 颜色选择、粗细调节、清空、撤销 | P0 |
| 答题阶段 | 与作画同时开始，非画家可答题 | P0 |
| 文字提示 | 显示答案字数 | P0 |
| 计分规则 | 最先猜中 + 画家各得 1 分 | P0 |
| 回合流转 | 回合结束自动进入下一回合 | P0 |
| 终局结算 | 弹出最终排名，累加总分 | P0 |
| 撤销画布 | 支持撤销上一步绘制笔触 | P1 |
| 橡皮擦工具 | 擦除画布内容 | P2 |

### 3.5 排行榜与记录

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 总排行榜 | 所有用户按总分降序 | P0 |
| 个人游戏记录 | 历史对局及每局得分 | P0 |
| 排行榜缓存 | Redis 缓存，减少数据库查询 | P2 |

### 3.6 管理员功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 用户审核 | 查看/通过/拒绝注册申请 | P0 |
| 用户管理 | 查看所有用户、注销用户 | P0 |
| 房间管理 | 查看在线房间、暂停/解散 | P0 |
| 词库管理 | 查看/新增/删除词语（5 个默认词不可删） | P0 |

### 3.7 新增功能 (相比原版)

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 🆕 密码 BCrypt 加密 | 替代原版明文密码 | P0 |
| 🆕 JWT 无状态认证 | 替代原版内存 Token | P0 |
| 🆕 Docker 部署 | docker-compose 一键启动 | P0 |
| 🆕 画布撤销 | 撤销上一步绘制 | P1 |
| 🆕 API 文档 | Swagger 在线接口文档 | P1 |
| 🆕 单元测试 | 覆盖核心逻辑 | P1 |
| 🆕 完整日志 | AOP 切面记录请求和异常 | P1 |

---

## 4. 数据库设计

### 4.1 E-R 图概览

```
用户 (User) ──< 好友 (Friend) >── 用户 (User)
  │
  ├──< 游戏记录 (GameRecord)
  │
  └──< 房间 (Room) >── 房间成员 (RoomMember)
                                      │
                                      └──< 游戏回合 (GameRound)
```

### 4.2 表结构

#### user (用户表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 主键 |
| phone | VARCHAR(11) UNIQUE | 手机号（登录标识） |
| nickname | VARCHAR(50) | 昵称 |
| password | VARCHAR(255) | BCrypt 加密密码 |
| role | ENUM('user','admin','super_admin') | 角色 |
| status | ENUM('pending','approved','rejected') | 审核状态 |
| total_score | INT DEFAULT 0 | 总得分 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### friend (好友关系表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 主键 |
| user_id | BIGINT (FK) | 用户 ID |
| friend_id | BIGINT (FK) | 好友 ID |
| status | ENUM('pending','accepted','rejected') | 请求状态 |
| created_at | DATETIME | 创建时间 |

#### room (房间表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 主键 |
| room_id | VARCHAR(6) UNIQUE | 6 位房间号 |
| creator_id | BIGINT (FK) | 房主 ID |
| state | ENUM('waiting','playing','paused','ended') | 房间状态 |
| created_at | DATETIME | 创建时间 |

#### room_member (房间成员表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 主键 |
| room_id | BIGINT (FK) | 房间 ID |
| user_id | BIGINT (FK) | 用户 ID |
| score | INT DEFAULT 0 | 本局得分 |
| painter_order | INT | 画家顺序 |

#### game_record (游戏记录表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 主键 |
| room_id | BIGINT (FK) | 房间 ID |
| played_at | DATETIME | 游戏时间 |

#### game_record_detail (游戏记录详情表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 主键 |
| record_id | BIGINT (FK) | 游戏记录 ID |
| user_id | BIGINT (FK) | 用户 ID |
| score | INT | 本局得分 |
| word | VARCHAR(50) | 本局分配词语 |

#### word (词库表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 主键 |
| word | VARCHAR(50) UNIQUE | 词语 |
| is_default | TINYINT(1) DEFAULT 0 | 是否默认词（不可删） |
| created_at | DATETIME | 创建时间 |

---

## 5. API 设计

### 5.1 REST API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/register | 无需 | 注册 |
| POST | /api/login | 无需 | 登录 |
| POST | /api/change-password | 无需(带token) | 首次修改密码 |
| GET | /api/user | JWT | 获取当前用户信息 |
| PUT | /api/user/nickname | JWT | 修改昵称 |
| PUT | /api/user/password | JWT | 修改密码 |
| DELETE | /api/user | JWT | 注销账户 |
| GET | /api/friends/search?q= | JWT | 搜索用户 |
| POST | /api/friends/request | JWT | 发送好友请求 |
| GET | /api/friends/requests | JWT | 查看好友请求 |
| POST | /api/friends/accept | JWT | 同意好友 |
| POST | /api/friends/reject | JWT | 拒绝好友 |
| DELETE | /api/friends/{friendId} | JWT | 删除好友 |
| POST | /api/rooms | JWT | 创建房间 |
| GET | /api/rooms/{roomId} | JWT | 查看房间信息 |
| POST | /api/rooms/{roomId}/join | JWT | 加入房间 |
| POST | /api/rooms/{roomId}/leave | JWT | 离开房间 |
| DELETE | /api/rooms/{roomId} | JWT(房主) | 解散房间 |
| GET | /api/leaderboard | JWT | 总排行榜 |
| GET | /api/user/games | JWT | 个人游戏记录 |
| GET | /api/admin/users | JWT(管理员) | 查看所有用户 |
| POST | /api/admin/approve | JWT(管理员) | 审核用户 |
| DELETE | /api/admin/users/{userId} | JWT(管理员) | 注销用户 |
| GET | /api/admin/words | JWT(管理员) | 词库列表 |
| POST | /api/admin/words | JWT(管理员) | 添加词语 |
| DELETE | /api/admin/words/{wordId} | JWT(管理员) | 删除词语 |
| GET | /api/admin/rooms | JWT(管理员) | 查看在线房间 |
| POST | /api/admin/rooms/{roomId}/pause | JWT(管理员) | 暂停房间 |
| POST | /api/admin/rooms/{roomId}/dismiss | JWT(管理员) | 解散房间 |

### 5.2 WebSocket 事件

| 事件方向 | 事件名 | 说明 |
|---------|--------|------|
| C→S | connect | 连接 WebSocket |
| C→S | auth | 发送 Token 认证 |
| C→S | join_room | 加入房间房间 |
| C→S | draw | 发送绘制数据 |
| C→S | submit_answer | 提交答案 |
| C→S | start_game | 房主开始游戏 |
| C→S | end_round | 结束当前回合 |
| S→C | auth_ok / auth_error | 认证结果 |
| S→C | user_joined / user_left | 成员加入/离开 |
| S→C | room_state | 房间状态同步 |
| S→C | round_start | 回合开始 |
| S→C | draw_data | 绘制数据广播 |
| S→C | answer_correct | 有人猜对了 |
| S→C | answer_result | 答题结果反馈 |
| S→C | game_end | 游戏结束结算 |
| S→C | room_paused / room_dismissed | 房间被暂停/解散 |

---

## 6. 项目结构

```
DrawGuess-v2.0/
├── draw-guess-server/             ← Maven 项目根目录
│   ├── src/main/java/com/drawguess/
│   │   ├── DrawGuessApplication.java      # 启动类
│   │   │
│   │   ├── config/                        # 配置类
│   │   │   ├── WebSocketConfig.java        # WebSocket 配置
│   │   │   ├── SecurityConfig.java         # Spring Security 配置
│   │   │   ├── RedisConfig.java            # Redis 配置
│   │   │   └── CorsConfig.java             # 跨域配置
│   │   │
│   │   ├── controller/                    # Controller 层 (REST API)
│   │   │   ├── UserController.java
│   │   │   ├── FriendController.java
│   │   │   ├── RoomController.java
│   │   │   ├── GameController.java
│   │   │   ├── LeaderboardController.java
│   │   │   └── AdminController.java
│   │   │
│   │   ├── websocket/                    # WebSocket 处理
│   │   │   ├── GameWebSocketHandler.java
│   │   │   └── WebSocketAuthInterceptor.java
│   │   │
│   │   ├── service/                      # Service 层 (业务逻辑)
│   │   │   ├── UserService.java
│   │   │   ├── FriendService.java
│   │   │   ├── RoomService.java
│   │   │   ├── GameService.java
│   │   │   ├── WordService.java
│   │   │   ├── LeaderboardService.java
│   │   │   └── AdminService.java
│   │   │
│   │   ├── mapper/                       # DAO 层 (MyBatis Mapper)
│   │   │   ├── UserMapper.java
│   │   │   ├── FriendMapper.java
│   │   │   ├── RoomMapper.java
│   │   │   ├── RoomMemberMapper.java
│   │   │   ├── GameRecordMapper.java
│   │   │   └── WordMapper.java
│   │   │
│   │   ├── model/entity/                 # 实体类
│   │   │   ├── User.java
│   │   │   ├── Friend.java
│   │   │   ├── Room.java
│   │   │   ├── RoomMember.java
│   │   │   ├── GameRecord.java
│   │   │   ├── GameRecordDetail.java
│   │   │   └── Word.java
│   │   │
│   │   ├── model/dto/                    # DTO (请求/响应)
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── UserInfoVO.java
│   │   │   └── GameRoomVO.java
│   │   │
│   │   ├── model/enums/                  # 枚举
│   │   │   ├── UserRole.java
│   │   │   ├── UserStatus.java
│   │   │   ├── RoomState.java
│   │   │   └── FriendStatus.java
│   │   │
│   │   ├── security/                     # 安全认证
│   │   │   ├── JwtTokenProvider.java      # JWT 工具
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   │
│   │   ├── common/                       # 公共工具
│   │   │   ├── ApiResponse.java           # 统一响应
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ResultCode.java
│   │   │
│   │   └── util/
│   │       └── RoomCodeGenerator.java     # 房间号生成器
│   │
│   ├── src/main/resources/
│   │   ├── application.yml                # 主配置
│   │   ├── application-dev.yml            # 开发环境
│   │   ├── application-prod.yml           # 生产环境
│   │   └── mapper/                        # MyBatis XML 映射
│   │       ├── UserMapper.xml
│   │       ├── FriendMapper.xml
│   │       ├── RoomMapper.xml
│   │       ├── RoomMemberMapper.xml
│   │       ├── GameRecordMapper.xml
│   │       └── WordMapper.xml
│   │
│   ├── src/main/resources/static/         # 前端静态文件
│   │   ├── index.html                     # 登录/注册页
│   │   ├── home.html                      # 首页
│   │   ├── room.html                      # 游戏房间
│   │   ├── admin.html                     # 管理员面板
│   │   ├── css/
│   │   └── js/
│   │
│   ├── docker/                           # Docker 相关
│   │   └── Dockerfile
│   │
│   └── pom.xml                           # Maven 依赖
│
├── docker-compose.yml                     # 一键启动
├── docs/
│   └── PRD.md                            # 本文档
├── README.md                              # 项目说明
└── AGENTS.md                              # 开发工作区规范
```

---

## 7. 非功能需求

### 7.1 性能
- 页面加载时间 ≤ 2 秒
- WebSocket 消息延迟 ≤ 100ms
- Redis 缓存命中率 ≥ 80%

### 7.2 安全
- 密码使用 BCrypt 加密存储
- JWT Token 鉴权，过期自动失效
- 接口防 XSS、防 SQL 注入
- 管理员操作权限校验

### 7.3 可维护性
- 统一响应格式 `ApiResponse<T>`
- 统一异常处理 `GlobalExceptionHandler`
- 完整日志（请求日志 + 异常日志）
- Swagger 接口文档自动生成

### 7.4 部署
- Docker + docker-compose 一键启动
- 支持 MySQL + Redis 容器化
- 环境配置分离（dev / prod）

---

## 8. 开发阶段规划

### 阶段一：基础架构搭建 (预计 3-5 天)
- [ ] 创建 Spring Boot 项目，配置 Maven 依赖
- [ ] 配置 MySQL + MyBatis-Plus + Redis
- [ ] 实现基础三层架构 (Controller/Service/Mapper)
- [ ] 配置统一响应和全局异常处理
- [ ] 配置 Spring Security + JWT

### 阶段二：用户系统 (预计 2-3 天)
- [ ] 用户注册、登录 API
- [ ] JWT Token 生成与校验
- [ ] 用户信息管理（修改昵称、密码、注销）
- [ ] 超级管理员初始化与强制改密
- [ ] 管理员审核功能

### 阶段三：好友系统 + 排行榜 (预计 1-2 天)
- [ ] 搜索用户、发送/处理好友请求
- [ ] 好友列表与在线状态
- [ ] 排行榜、个人游戏记录

### 阶段四：房间系统 + WebSocket (预计 3-5 天)
- [ ] Spring WebSocket 配置与认证
- [ ] 房间创建、加入、离开
- [ ] 管理员房间管理（暂停、解散）

### 阶段五：游戏核心逻辑 (预计 5-7 天)
- [ ] 游戏状态机（等待→游戏中→结束）
- [ ] 画家分配与回合流转
- [ ] 画布绘制同步（WebSocket 广播）
- [ ] 答题逻辑与计分
- [ ] 终局结算

### 阶段六：词库管理 + 管理后台 (预计 1-2 天)
- [ ] 词库 CRUD
- [ ] 管理员控制台接口

### 阶段七：前端页面适配 (预计 2-3 天)
- [ ] 适配原版前端 HTML 页面到新后端
- [ ] 登录/注册页、首页、游戏房间、管理后台

### 阶段八：Docker 部署 + 测试 (预计 1-2 天)
- [ ] Dockerfile + docker-compose.yml
- [ ] 核心逻辑单元测试
- [ ] 端到端功能验证

---

## 9. 版本迭代规划

| 版本 | 交付内容 | 预计工时 |
|------|---------|---------|
| v2.0-alpha | 阶段一 ~ 四（基础架构 + 用户 + 好友 + 房间） | 8-12 天 |
| v2.0-beta | 阶段五 ~ 六（游戏核心 + 词库） | 6-9 天 |
| v2.0-rc | 阶段七 ~ 八（前端适配 + Docker + 测试） | 3-5 天 |
| v2.0-release | 完整可用版本 | 总计约 20-25 天 |

---

## 10. 附录

### 10.1 与原版功能差异摘要

| 模块 | 原版 v1.0 | v2.0 升级 |
|------|----------|----------|
| 密码存储 | 明文 | BCrypt 加密 |
| 认证方式 | 内存 Token | JWT 无状态 |
| 数据存储 | JSON 文件 | MySQL 数据库 |
| 缓存 | 无 | Redis |
| 画布功能 | 基础绘制 | 新增撤销功能 |
| 部署 | 手动运行 | Docker 一键部署 |
| 代码规范 | Python 单文件 | Java 分层架构 |
| 测试 | 无 | JUnit 单元测试 |

### 10.2 风险与应对

| 风险 | 应对方案 |
|------|---------|
| WebSocket 学习曲线 | 先从简单广播开始，逐步添加复杂逻辑 |
| 前端适配工作量 | 复用原版前端 HTML，只改 API 通信层 |
| 游戏状态一致性 | Redis 缓存房间状态，提供状态恢复机制 |
