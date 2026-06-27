# 阶段一：基础架构搭建 — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段一

---

## 一、本阶段完成的工作

### 1. 创建 Maven 项目结构

从零搭建了完整的 Spring Boot 项目骨架，目录结构严格遵循 PRD 第 6 章设计：

```
draw-guess-server/
├── pom.xml                          ← Maven 依赖管理
├── docker/
│   └── init.sql                     ← 数据库初始化脚本
└── src/
    ├── main/java/com/drawguess/
    │   ├── DrawGuessApplication.java    ← 启动类
    │   ├── config/                      ← 配置类 (6个)
    │   ├── controller/                  ← 控制器 (6个)
    │   ├── websocket/                   ← WebSocket (待阶段四实现)
    │   ├── service/                     ← Service接口 (7个)
    │   │   └── impl/                    ← Service实现 (7个)
    │   ├── mapper/                      ← Mapper接口 (6个)
    │   ├── model/
    │   │   ├── entity/                  ← 实体类 (7个)
    │   │   ├── dto/                     ← DTO/VO (6个)
    │   │   └── enums/                   ← 枚举 (4个)
    │   ├── security/                    ← JWT安全 (3个)
    │   ├── common/                      ← 公共组件 (4个)
    │   └── util/                        ← 工具类 (1个)
    └── main/resources/
        ├── application.yml              ← 主配置
        ├── application-dev.yml          ← 开发环境
        ├── application-prod.yml         ← 生产环境
        └── mapper/                      ← MyBatis XML (6个)
```

### 2. Maven 依赖配置 (pom.xml)

关键决策：
- **Parent**: `spring-boot-starter-parent:3.2.0`，统一管理 Spring 全家桶版本
- **版本集中管理**: 在 `<properties>` 中统一声明 mybatis-plus、jjwt、springdoc 版本号
- **核心依赖**: web、websocket、security、redis、validation 五大 starter
- **MyBatis-Plus**: 使用 `mybatis-plus-spring-boot3-starter`（注意是 Boot3 专用版本）
- **JWT**: jjwt 三件套（api + impl + jackson），运行时范围正确
- **Lombok**: 设为 optional，打包时排除

### 3. 配置文件

| 文件 | 职责 | 关键配置 |
|------|------|---------|
| `application.yml` | 主配置 | Jackson 日期格式、MyBatis-Plus 下划线转驼峰、JWT 密钥/过期时间、Swagger 路径 |
| `application-dev.yml` | 开发环境 | 本地 MySQL/Redis 连接、SQL 日志打印 |
| `application-prod.yml` | 生产环境 | Docker 内 MySQL/Redis 连接、环境变量注入密码、关闭 SQL 日志 |

### 4. 数据库初始化 (init.sql)

- 7 张表完整建表语句，字段类型、索引、注释齐全
- `user` 表 `phone` 字段设唯一索引
- `room` 表 `room_id` 设唯一索引
- `word` 表 `word` 设唯一索引
- 插入 5 个默认词语（`is_default=1`）
- 使用 `utf8mb4` 字符集，支持中文和 emoji

### 5. 实体类设计

- 所有实体继承 MyBatis-Plus 注解体系：`@TableName`、`@TableId(AUTO)`、`@EnumValue`、`@TableField(fill)`
- 枚举字段使用 `@EnumValue` 注解，MyBatis-Plus 自动处理枚举与字符串的映射
- `createdAt`/`updatedAt` 使用自动填充（`FieldFill.INSERT`/`INSERT_UPDATE`）
- 枚举类：`UserRole`、`UserStatus`、`RoomState`、`FriendStatus`

### 6. 公共组件

| 类 | 职责 |
|----|------|
| `ApiResponse<T>` | 统一响应包装，静态工厂方法 `success()`/`error()` |
| `ResultCode` | 错误码枚举，按模块分段（用户1xxx、好友2xxx、房间3xxx、游戏4xxx、词库5xxx、通用9xxx） |
| `BusinessException` | 业务异常，携带 `ResultCode` |
| `GlobalExceptionHandler` | 全局异常处理：BusinessException → 参数校验 → 认证 → 兜底 |

### 7. Security 安全体系

| 类 | 职责 |
|----|------|
| `JwtTokenProvider` | JWT 生成/解析/验证，使用 HMAC-SHA 签名 |
| `JwtAuthenticationFilter` | 每次请求拦截，从 Header 提取 Token，验证后设置 SecurityContext |
| `UserDetailsServiceImpl` | Spring Security 的 UserDetailsService 实现 |
| `SecurityConfig` | 安全配置：无状态 Session、CSRF 禁用、路径权限（公开/认证/管理员） |

### 8. 其他 Config

| 类 | 职责 |
|----|------|
| `RedisConfig` | RedisTemplate 序列化配置（Key:String、Value:JSON） |
| `CorsConfig` | 跨域配置（允许所有来源，开发阶段） |
| `WebSocketConfig` | STOMP WebSocket 配置（/ws 端点、/topic+/queue 代理、/app 前缀） |
| `MyBatisMetaObjectHandler` | 自动填充 createdAt/updatedAt |
| `DataInitConfig` | 应用启动时自动初始化超级管理员 |

---

## 二、搭建顺序与流程总结

阶段一的核心工作顺序，反映了 Spring Boot 项目的**自底向上**搭建思路：

```
1. 项目骨架 → pom.xml + 目录结构
      ↓
2. 配置文件 → application.yml (主/开发/生产)
      ↓
3. 数据层 → 建表SQL → Entity → Enum → Mapper接口 → Mapper XML
      ↓
4. 公共组件 → ApiResponse → ResultCode → BusinessException → GlobalExceptionHandler
      ↓
5. 安全层 → JwtTokenProvider → JwtAuthenticationFilter → UserDetailsServiceImpl
      ↓
6. 配置类 → SecurityConfig → RedisConfig → CorsConfig → WebSocketConfig
      ↓
7. 业务层 → Service接口 → ServiceImpl (含核心业务逻辑)
      ↓
8. 控制层 → Controller (REST API 骨架)
      ↓
9. 初始化 → DataInitConfig (超级管理员自动创建)
      ↓
10. 验证 → mvn clean compile
```

**关键原则**：
- **先数据后业务**：先建表和实体，再写业务逻辑
- **先通用后具体**：先写公共组件（响应/异常），再写具体业务
- **先安全后接口**：先配好 Security + JWT，再暴露 API
- **接口先行**：先定义 Service 接口，再写实现，便于解耦

---

## 三、架构决策记录

### 3.1 枚举存储策略
**决策**：使用 `@EnumValue` + VARCHAR 存储，而非 MySQL ENUM 类型
**原因**：
- MyBatis-Plus 的 `@EnumValue` 注解自动映射，代码侧类型安全
- VARCHAR 比 MySQL ENUM 更灵活，增加枚举值不需要 DDL 变更
- MySQL ENUM 在 Java 侧不直观，容易出问题

### 3.2 JWT 密钥管理
**决策**：初期硬编码在 yml 中，生产环境建议迁移到环境变量
**原因**：MVP 阶段简化配置，`@Value` 注入即可

### 3.3 WebSocket 选型
**决策**：使用 STOMP 协议而非原生 WebSocket
**原因**：
- STOMP 提供订阅/发布语义，天然适合房间广播
- Spring 对 STOMP 有完整支持（`@MessageMapping`、`SimpMessagingTemplate`）
- 便于后续扩展点对点消息

### 3.4 统一响应格式
**决策**：所有 API 返回 `ApiResponse<T>`，错误码用自定义数字码
**原因**：HTTP 状态码不够细粒度（如 400 无法区分"手机号已注册"和"密码错误"），自定义错误码更精确

---

## 四、待后续阶段完善的部分

| 项目 | 当前状态 | 计划阶段 |
|------|---------|---------|
| WebSocket 认证拦截器 | 未实现 | 阶段四 |
| 游戏核心逻辑 (GameService) | 骨架+TODO | 阶段五 |
| Redis 缓存（排行榜/房间状态） | 配置好但未使用 | 阶段三/五 |
| 前端静态页面 | 空目录 | 阶段七 |
| Docker 部署文件 | 仅 init.sql | 阶段八 |
| 单元测试 | 空目录 | 阶段八 |
| 编译验证 | 未执行（环境无 Maven） | 下次启动时验证 |

---

## 五、文件清单

共创建 **46 个文件**：

- 配置/构建：3 (pom.xml, application.yml×3)
- Java 源码：37
  - 启动类：1
  - Config：5 (Security, Redis, CORS, WebSocket, DataInit, MetaObjectHandler)
  - Entity：7 (User, Friend, Room, RoomMember, GameRecord, GameRecordDetail, Word)
  - DTO/VO：6 (LoginRequest, RegisterRequest, UserInfoVO, GameRoomVO, ChangePassword, ChangeNickname)
  - Enum：4 (UserRole, UserStatus, RoomState, FriendStatus)
  - Mapper：6 (接口) + 6 (XML)
  - Service：7 (接口) + 7 (实现)
  - Controller：6
  - Security：3 (JwtTokenProvider, Filter, UserDetailsService)
  - Common：4 (ApiResponse, ResultCode, BusinessException, GlobalExceptionHandler)
  - Util：1 (RoomCodeGenerator)
- SQL 脚本：1 (init.sql)
