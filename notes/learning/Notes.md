# DrawGuess v2.0 学习笔记

## Docker 部署 vs 其他部署方式

### 三种部署方式对比

| | Docker Compose | 本地直接运行 | 云服务器 JAR 部署 |
|---|---|---|---|
| **启动** | `docker-compose up -d` 一键搞定 | 需手动装 JDK/MySQL/Redis | 需手动配置服务器环境 |
| **环境隔离** | 完全隔离，不污染本地 | 和本地环境混在一起 | 隔离但需手动管理 |
| **跨平台** | Win/Mac/Linux 完全一致 | 环境差异大 | 只在服务器上跑 |
| **团队协作** | 他人 clone 后直接跑 | 每人自己装环境 | N/A |
| **改代码后** | 需 `--build` 重建镜像（慢，约 10-30 秒） | `mvn spring-boot:run` 秒级重启 | 重新上传 JAR 重启 |
| **资源占用** | 镜像占磁盘（约 4GB+），容器额外占内存 | 最轻量 | 中等 |

### 痛点：改代码后要重新 build

Docker 开发阶段最大的问题：每次改代码都要执行 `docker-compose up -d --build`，Maven 重新编译 + 构建镜像，耗时较长。

**原因**：Docker 镜像是不可变的。代码修改后必须重新构建镜像，才能把新代码打包进容器。

### 解决方案：混合开发模式

Docker 只跑 MySQL/Redis，Spring Boot 本地运行：

```bash
# 1. 只启动数据库和缓存
docker-compose up -d mysql redis

# 2. 本地跑 Spring Boot（改代码后秒级重启）
cd draw-guess-server
mvn spring-boot:run
```

这样改代码后只需重启 Spring Boot，不用等 Docker 重新构建。

**注意**：本地运行时使用 `application-dev.yml` 配置，确保数据库地址指向 `localhost`（Docker 映射的端口 3307）。

### 不同阶段的推荐方式

| 阶段 | 推荐方式 | 原因 |
|------|---------|------|
| 日常开发 | Docker 跑 MySQL/Redis + 本地 `mvn spring-boot:run` | 改代码秒重启，开发效率最高 |
| 测试验收 | `docker-compose up -d --build` 完整验证 | 确保生产环境一致性 |
| 正式部署 | Docker 或直接 JAR 部署到云服务器 | 稳定、可扩展 |

### 常用 Docker 命令速查

```bash
# 完整启动（含构建）
docker-compose up -d --build

# 只启动部分服务
docker-compose up -d mysql redis

# 查看容器状态
docker-compose ps

# 查看服务日志
docker-compose logs -f server

# 停止所有服务
docker-compose down

# 停止并删除数据卷（重置数据库）
docker-compose down -v

# 清理无用镜像和缓存
docker system prune -f

# 查看磁盘占用
docker system df
```

---

## Swagger API 文档

### 什么是 Swagger

Swagger 是一个自动生成 REST API 文档的工具。项目集成了 SpringDoc OpenAPI（Swagger 3.0），它会扫描所有 Controller 中的接口，自动生成一份可交互的 API 文档页面。

访问地址：`http://localhost:8080/swagger-ui.html`

### 它能做什么

| 功能 | 说明 |
|------|------|
| **查看所有接口** | 列出项目中所有的 REST API，按 Controller 分组 |
| **查看参数和返回值** | 每个接口的请求参数、类型、是否必填、返回数据结构一目了然 |
| **在线测试** | 直接在页面上填参数、点 Execute 发送请求，实时看到响应结果 |
| **生成文档** | 可导出 JSON/YAML 格式的 API 规范文件，供前端或第三方对接 |

### 实际使用示例

以 DrawGuess 为例，打开 `http://localhost:8080/swagger-ui.html` 后你可以：

1. 找到 `user-controller` → `POST /api/login` → 点击展开
2. 看到 `LoginRequest` 需要哪些字段（phone、password）
3. 点击「Try it out」，填入手机号和密码
4. 点击「Execute」，直接发送登录请求
5. 查看返回的 JSON 响应

不需要 Postman，不需要写 curl 命令，浏览器里就能测试所有接口。

### 项目中的技术实现

```
Controller 中写的接口
        ↓
SpringDoc 自动扫描
        ↓
生成 OpenAPI 规范（JSON: /v3/api-docs）
        ↓
Swagger UI 渲染成可视化页面（/swagger-ui.html）
```

相关依赖（pom.xml）：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

### 与 Postman 的区别

| | Swagger UI | Postman |
|---|---|---|
| 文档生成 | 代码驱动，自动生成 | 手动编写/导入 |
| 测试方式 | 浏览器内直接测 | 独立应用 |
| 团队共享 | 随应用启动即可访问 | 需导出 Collection 分享 |
| 适用场景 | 开发阶段快速调试 | 复杂的接口测试和自动化 |

> 简单说：Swagger 是**边写代码边出文档**，Postman 是**手动建测试集**。开发时用 Swagger 更方便。

---

## REST 是什么

### 一句话解释

REST（Representational State Transfer）是一种**设计 Web API 的风格规范**，不是框架、不是库，而是一套约定。

遵循 REST 风格的 API 叫 **RESTful API**，核心思想：**用 URL 表示资源，用 HTTP 方法表示操作**。

### 核心规则

| HTTP 方法 | 操作 | 示例 | 说明 |
|-----------|------|------|------|
| GET | 查询 | `GET /api/admin/words` | 获取词库列表 |
| POST | 新增 | `POST /api/admin/words` | 添加新词语 |
| PUT | 修改 | `PUT /api/user/nickname` | 修改昵称 |
| DELETE | 删除 | `DELETE /api/admin/words/3` | 删除 ID 为 3 的词 |

### 本项目中的 RESTful 设计

```
用户相关：
  POST   /api/register          ← 注册
  POST   /api/login             ← 登录
  GET    /api/user              ← 获取当前用户信息
  PUT    /api/user/nickname     ← 修改昵称
  PUT    /api/user/password     ← 修改密码
  DELETE /api/user              ← 注销账号

好友相关：
  GET    /api/friends           ← 好友列表
  GET    /api/friends/search    ← 搜索用户
  POST   /api/friends/request   ← 发送好友请求
  POST   /api/friends/accept    ← 接受请求
  DELETE /api/friends/{id}      ← 删除好友

词库管理：
  GET    /api/admin/words       ← 获取所有词
  POST   /api/admin/words       ← 添加词
  DELETE /api/admin/words/{id}  ← 删除词
```

### REST vs 非 REST 对比

| | RESTful 风格 | 非 REST 风格 |
|---|---|---|
| 获取用户 | `GET /api/users/1` | `POST /getUserById?id=1` |
| 删除词语 | `DELETE /api/words/3` | `POST /deleteWord?id=3` |
| 修改昵称 | `PUT /api/user/nickname` | `POST /updateNickname` |

区别：REST 用 HTTP 方法区分操作，URL 只表示资源；非 REST 全用 POST，靠 URL 路径或参数区分操作。

### 简单记忆

- URL 里放**名词**（资源）：`/users`、`/words`、`/rooms`
- HTTP 方法表示**动词**（操作）：GET 查、POST 增、PUT 改、DELETE 删
- 不在 URL 里写动词：`/deleteUser`、`/getWord` 这些不符合 REST
