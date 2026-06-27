# DrawGuess v2.0 — 你画我猜

> 📌 原 Python 版 → Java 重写升级版  
> 一款多人实时协作的「你画我猜」Web 系统，前后端分离，支持房间创建、好友邀请、实时作画与竞猜。

---

## ✨ 功能特性

### 🎮 游戏核心
- **实时画布**：鼠标/触摸绘制，多色可选，粗细调节，支持撤销
- **智能出题**：从词库随机抽取，30 秒作画 + 答题同时进行
- **公平赛制**：每人轮流当画家，最先猜中者与画家各得 1 分
- **自动结算**：所有回合结束后弹出排名，分数自动累加

### 👥 社交系统
- **好友系统**：搜索添加、请求处理、在线状态查看
- **房间系统**：6 位房间号一键加入，支持 3 人以上开局
- **排行榜**：全局总分排名，个人历史战绩查询

### 🔐 管理后台
- **用户审核**：注册需管理员审批
- **词库管理**：5 个默认词不可删，可自由增删自定义词
- **房间管理**：管理员可暂停/解散违规房间

### 🚀 相比 v1.0 升级亮点
| 维度 | v1.0 (Python) | v2.0 (Java) | 提升 |
|------|---------------|-------------|------|
| 技术栈 | Python Flask | Java Spring Boot | 🎯 匹配岗位需求 |
| 数据存储 | JSON 文件 | MySQL + MyBatis-Plus | 💾 可靠持久化 |
| 认证 | 明文密码 + 内存 Token | BCrypt + JWT | 🔒 企业级安全 |
| 缓存 | 无 | Redis | ⚡ 高性能 |
| 实时通信 | Flask-SocketIO | Spring WebSocket / STOMP | 📡 标准化 |
| 代码架构 | 单文件模块 | Controller/Service/DAO 分层 | 🏗️ 规范可维护 |
| 部署 | 手动运行 | Docker + docker-compose | 🐳 一键启动 |
| 测试 | 无 | JUnit 5 | ✅ 质量保障 |

---

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.x |
| 数据库 | MySQL 8.x |
| ORM | MyBatis-Plus |
| 缓存 | Redis 7.x |
| 实时通信 | Spring WebSocket / STOMP |
| 安全认证 | Spring Security + JWT |
| 密码加密 | BCrypt |
| 构建工具 | Maven |
| API 文档 | SpringDoc OpenAPI (Swagger) |
| 日志 | SLF4J + Logback |
| 前端 | 原生 HTML + CSS + JavaScript |
| 部署 | Docker + docker-compose |
| 测试 | JUnit 5 + Mockito |

---

## 📁 项目结构

```
DrawGuess-v2.0/
├── draw-guess-server/              ← Maven 项目
│   ├── src/main/java/com/drawguess/
│   │   ├── config/                  # 配置（WebSocket/Security/Redis/CORS）
│   │   ├── controller/              # REST API 控制器
│   │   ├── websocket/               # WebSocket 处理器
│   │   ├── service/                 # 业务逻辑层
│   │   ├── mapper/                  # MyBatis Mapper 接口
│   │   ├── model/                   # 实体 / DTO / 枚举
│   │   ├── security/                # JWT + Spring Security
│   │   ├── common/                  # 统一响应 / 异常处理
│   │   └── util/                    # 工具类
│   │
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置
│   │   ├── mapper/                   # MyBatis XML
│   │   └── static/                   # 前端页面
│   │       ├── index.html            # 登录/注册
│   │       ├── home.html             # 首页
│   │       ├── room.html             # 游戏房间
│   │       └── admin.html            # 管理后台
│   │
│   └── pom.xml
│
├── docker-compose.yml                # 一键部署
├── docs/
│   └── PRD.md                        # 产品需求文档
├── README.md                         # 本文件
└── AGENTS.md                         # 开发工作区规范
```

---

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- Docker & docker-compose（推荐）或 MySQL 8.x + Redis 7.x 本地安装

### 方式一：Docker 一键启动（推荐）

```bash
# 克隆项目
cd DrawGuess-v2.0

# 一键启动
docker-compose up -d

# 访问
# http://localhost:8080
```

### 方式二：本地开发

```bash
# 1. 确保本地 MySQL + Redis 已启动

# 2. 修改配置
# draw-guess-server/src/main/resources/application-dev.yml

# 3. 编译运行
cd draw-guess-server
mvn clean compile
mvn spring-boot:run

# 4. 访问
# http://localhost:8080
```

### 默认管理员账号
| 账号 | 说明 |
|------|------|
| 手机号：`00000000000` | 超级管理员 |
| 密码：`admin123` | 首次登录强制修改 |

---

## 📖 使用说明

### 管理员操作
1. 用 `00000000000 / admin123` 登录（首次登录需修改密码）
2. 首页右上角进入"管理员控制台"
3. **用户审核**：批准/拒绝注册申请
4. **词库管理**：增删自定义词语
5. **房间管理**：查看/暂停/解散在线房间

### 普通用户操作
1. **注册**：填写昵称、手机号、密码，等待管理员审核
2. **登录**：审核通过后登录
3. **添加好友**：搜索好友并发送请求
4. **创建房间**：生成 6 位房间号，分享给好友
5. **开始游戏**：≥ 3 人后房主点击开始
6. **游戏流程**：当画家就画，当观众就猜

---

## 🧪 Api 文档

启动项目后访问：
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/v3/api-docs`

---

## 📊 数据库设计

核心表结构：

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `user` | 用户表 | phone(唯一), nickname, password(BCrypt), role, status, total_score |
| `friend` | 好友关系 | user_id, friend_id, status |
| `room` | 房间表 | room_id(6位), creator_id, state |
| `room_member` | 房间成员 | room_id, user_id, score, painter_order |
| `game_record` | 游戏记录 | room_id, played_at |
| `game_record_detail` | 记录详情 | record_id, user_id, score, word |
| `word` | 词库 | word(唯一), is_default |

详见 [docs/PRD.md](./docs/PRD.md) 第 4 章。

---

## 📋 开发计划

| 阶段 | 内容 | 预计 |
|------|------|------|
| 一 | 基础架构搭建（Spring Boot + MySQL + Redis） | 3-5 天 |
| 二 | 用户系统（注册登录 + JWT + BCrypt） | 2-3 天 |
| 三 | 好友系统 + 排行榜 | 1-2 天 |
| 四 | 房间系统 + WebSocket 实时通信 | 3-5 天 |
| 五 | 游戏核心逻辑（状态机 + 画布 + 答题 + 计分） | 5-7 天 |
| 六 | 词库管理 + 管理后台 | 1-2 天 |
| 七 | 前端页面适配 | 2-3 天 |
| 八 | Docker 部署 + 单元测试 | 1-2 天 |

---

## 🤝 贡献

欢迎提 Issue 或 PR！

---

## 📄 许可

MIT License