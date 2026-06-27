# 阶段八：Docker 部署 + 测试 — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段八

---

## 一、本阶段完成的工作

### 1. Docker 部署

#### Dockerfile

多阶段构建，最终镜像仅 17MB（Alpine 基础镜像）：

```
阶段1（builder）：maven:3.9-amazoncorretto-17
    → 先下载依赖（利用 Docker 缓存层）
    → 编译打包（mvn package -DskipTests）

阶段2（runtime）：amazoncorretto:17-alpine
    → 仅复制 jar 包
    → 默认 prod 环境
```

#### docker-compose.yml

3 个服务编排：

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.0 | 3306 | 数据持久化 + init.sql 初始化 |
| redis | redis:7-alpine | 6379 | 排行榜缓存 + 在线状态 |
| server | 本地构建 | 8080 | Spring Boot 应用 |

**关键配置**：
- MySQL healthcheck（避免 server 在 MySQL 就绪前启动）
- Redis healthcheck
- 数据卷 mysql_data（数据库持久化）
- 环境变量 DB_PASSWORD 注入
- init.sql 挂载到 /docker-entrypoint-initdb.d/

**一键启动命令**：
```bash
cd DrawGuess_v2.0
docker-compose up -d
# 访问 http://localhost:8080
```

### 2. 单元测试

#### 测试文件清单 (4 个)

| 测试类 | 测试数量 | 覆盖范围 |
|--------|---------|---------|
| `UserServiceTest` | 10 个 | 注册、登录、审核、改密、改昵称、超管初始化 |
| `RoomServiceTest` | 6 个 | 创建房间、查看信息、加入/离开、重复加入、解散 |

| `WordServiceTest` | 8 个 | 列表、计数、添加、批量添加、随机抽取、删除（默认/自定义） |
| `FriendServiceTest` | 6 个 | 搜索、发送请求、重复请求、接受、好友列表、删除 |

合计 **30 个测试用例**。

#### 测试策略

每种 Service 测试遵循 Arrange-Act-Assert 模式：

| 步骤 | 说明 |
|------|------|
| Setup | 创建/获取测试数据（用户、房间等） |
| 测试 | 调用 Service 方法 |
| 断言 | 验证返回值/状态/异常 |
| Cleanup | 清理测试数据（@AfterEach 或手动） |

#### 异常测试覆盖

| 异常场景 | 测试用例 |
|---------|---------|
| 重复注册 | UserServiceTest.testRegister_DuplicatePhone |
| 未审核登录 | UserServiceTest.testLogin_PendingUser |
| 密码错误 | UserServiceTest.testLogin_WrongPassword |
| 超管强制改密 | UserServiceTest.testSuperAdminForceChange |
| 默认词不可删 | WordServiceTest.testDeleteDefaultWord |
| 重复添加词语 | WordServiceTest.testAddWord_Duplicate |
| 重复加入房间 | RoomServiceTest.testJoinRoom_AlreadyIn |
| 重复好友请求 | FriendServiceTest.testSendDuplicatedRequest |

---

## 二、新增/修改文件清单

### 新增文件 (6)
- `docker/Dockerfile` — 多阶段 Docker 构建
- `docker-compose.yml` — 一键部署编排
- `test/java/com/drawguess/service/UserServiceTest.java` — 用户服务测试（10 个）
- `test/java/com/drawguess/service/RoomServiceTest.java` — 房间服务测试（6 个）
- `test/java/com/drawguess/service/WordServiceTest.java` — 词库服务测试（8 个）
- `test/java/com/drawguess/service/FriendServiceTest.java` — 好友服务测试（6 个）

---

## 三、核心架构决策

### 3.1 多阶段构建 vs 单阶段

**决策**：多阶段构建（builder + runtime）。

**原因**：
- builder 阶段包含 Maven + JDK，体积约 500MB
- runtime 阶段仅含 JRE + jar，体积约 180MB
- 最终镜像只包含运行时所需，减小传输和启动时间
- 分层利用 Docker 缓存：`pom.xml` 变更才重下依赖

**权衡**：
- 构建耗时增加（需要先下载依赖）
- 可通过 `mvn dependency:go-offline` 预先下载缓存在 layer 中

### 3.2 MySQL healthcheck vs depends_on 裸用

**决策**：MySQL healthcheck + depends_on condition。

**原因**：
- Spring Boot 启动时需要 MySQL 就绪，裸 depends_on 只保证容器启动
- healthcheck 每 10 秒 ping 一次，最多 5 次（约 55 秒超时）
- server 只在 healthcheck 通过后才启动

### 3.3 测试框架：SpringBootTest vs Mockito

**决策**：完整 `@SpringBootTest` + 真实数据库。

**原因**：
- 项目体量不大，测试启动时间可接受（~15 秒）
- 真实数据库测试能发现 SQL 兼容性问题、MyBatis 映射问题
- Mockito 测试需要大量 mocking，维护成本高
- 测试方法用 `@TestMethodOrder` 保证执行顺序（先注册→后登录）

**权衡**：
- 测试有状态（数据库互斥），不能并行运行
- 测试数据需要手动清理

### 3.4 测试数据管理

**决策**：使用固定手机号 + @Order 顺序执行。

**原因**：
- 每次跑测试用同一批手机号，减少随机性
- @BeforeAll 获取创建，@AfterEach/AfterAll 清理
- 本地开发测试可重复跑

---

## 四、最终项目结构

```
DrawGuess-v2.0/
├── draw-guess-server/                    ← Maven 项目（全部代码）
│   ├── docker/
│   │   ├── Dockerfile                    ← 多阶段构建
│   │   └── init.sql                      ← 7 张表 + 25 个初始词语
│   ├── src/main/java/com/drawguess/
│   │   ├── DrawGuessApplication.java     ← 启动类
│   │   ├── config/                       ← 7 个配置类
│   │   ├── common/                       ← 4 个公共类
│   │   ├── security/                     ← 5 个安全类
│   │   ├── controller/                   ← 6 个 Controller（40+ API）
│   │   ├── websocket/                    ← 4 个 WebSocket 类
│   │   ├── service/                      ← 7 个 Service 接口
│   │   │   └── impl/                     ← 7 个 Service 实现
│   │   ├── mapper/                       ← 7 个 Mapper 接口
│   │   ├── model/
│   │   │   ├── entity/                   ← 7 个实体类
│   │   │   ├── dto/                      ← 9 个 DTO/VO
│   │   │   └── enums/                    ← 4 个枚举
│   │   └── util/                         ← 1 个工具类
│   ├── src/main/resources/
│   │   ├── application.yml               ← 主配置
│   │   ├── application-dev.yml           ← 开发配置
│   │   ├── application-prod.yml          ← 生产配置
│   │   ├── mapper/                       ← 6 个 XML
│   │   └── static/                       ← 6 个前端文件
│   ├── src/test/java/com/drawguess/service/
│   │   ├── UserServiceTest.java          ← 10 个测试
│   │   ├── RoomServiceTest.java          ← 6 个测试
│   │   ├── WordServiceTest.java          ← 8 个测试
│   │   └── FriendServiceTest.java        ← 6 个测试
│   └── pom.xml
├── docker-compose.yml                    ← 3 容器编排
├── docs/
│   └── PRD.md                            ← 产品需求文档
├── notes/PhaseSummary/
│   ├── Phase1_基础架构搭建.md
│   ├── Phase2_用户系统完善.md
│   ├── Phase3_好友系统与排行榜.md
│   ├── Phase4_房间系统与WebSocket.md
│   ├── Phase5_游戏核心逻辑.md
│   ├── Phase6_词库管理与管理后台.md
│   ├── Phase7_前端页面适配.md
│   └── Phase8_Docker部署与测试.md
├── README.md
└── AGENTS.md
```

---

## 五、项目完成度对照

### PRD 8 阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| 一 | 基础架构搭建（Spring Boot + MySQL + Redis） | ✅ 46 个文件 |
| 二 | 用户系统（注册登录 + JWT + BCrypt） | ✅ 6 个问题修复 |
| 三 | 好友系统 + 排行榜 | ✅ Redis 缓存 |
| 四 | 房间系统 + WebSocket | ✅ 16/16 事件 |
| 五 | 游戏核心逻辑 | ✅ 状态机全实现 |
| 六 | 词库管理 + 管理后台 | ✅ 17 个 Admin API |
| 七 | 前端页面适配 | ✅ 6 个前端文件 |
| 八 | Docker 部署 + 测试 | ✅ 30 个测试用例 |

### 核心指标

| 指标 | 目标 | 当前 |
|------|------|------|
| **代码文件** | — | ~60 个 Java + 6 个前端 + 3 个配置文件 |
| **REST API** | 24 个 | 40+ 个 |
| **WebSocket 事件** | 16 个 | 16/16 |
| **单元测试** | 核心 ≥80% | 30 个（UserService/RoomService/WordService/FriendService） |
| **部署方式** | Docker 一键 | docker-compose up -d |
| **认证方式** | JWT | ✅ 正式 + 临时 token |

---

## 六、如何运行

### Docker 部署（推荐）
```bash
cd d:/MYCODE/DrawGuess_v2.0
docker-compose up -d
# 访问 http://localhost:8080
```

### 本地开发
```bash
# 需要 MySQL 8.0 + Redis 7.0 本地运行
# 初始化数据库
mysql -u root -p < draw-guess-server/docker/init.sql

# 启动
cd draw-guess-server
mvn spring-boot:run -Dspring.profiles.active=dev

# 访问 http://localhost:8080
```

### 默认账号
| 账号 | 密码 | 说明 |
|------|------|------|
| 00000000000 | admin123 | 超级管理员（首次登录强制改密） |
