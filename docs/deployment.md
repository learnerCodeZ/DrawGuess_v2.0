# DrawGuess v2.0 部署指南

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 本地开发时需要 |
| Maven | 3.8+ | 本地开发时需要 |
| Docker | 20.10+ | Docker 部署时需要 |
| Docker Compose | v2+ | Docker 部署时需要 |
| MySQL | 8.x | 本地开发时需要 |
| Redis | 7.x | 本地开发时需要 |

---

## 方式一：Docker Compose 一键部署（推荐）

最简方式，无需本地安装 JDK / Maven / MySQL / Redis。

### 1. 克隆项目

```bash
git clone <仓库地址>
cd DrawGuess_v2.0
```

### 2. 一键启动

```bash
docker-compose up -d
```

该命令会自动完成：
- 启动 MySQL 8.0 容器，自动执行 `init.sql` 初始化数据库和表结构
- 启动 Redis 7 容器
- 构建并启动 Spring Boot 后端服务（多阶段构建：Maven 编译 → JRE 运行）

### 3. 验证部署

```bash
# 查看容器状态
docker-compose ps

# 查看服务日志
docker-compose logs -f server
```

服务启动成功后访问：
- 应用首页：`http://localhost:8080`
- Swagger API 文档：`http://localhost:8080/swagger-ui.html`

### 4. 停止服务

```bash
docker-compose down

# 如需同时删除数据卷（清空数据库数据）
docker-compose down -v
```

### 5. 自定义配置

可修改 `docker-compose.yml` 中的环境变量：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring 配置文件，不建议修改 |
| `DB_PASSWORD` | `drawguess123` | MySQL 数据库密码，需与 MySQL 容器配置一致 |

如需修改端口映射，编辑 `docker-compose.yml` 中 `ports` 字段：

```yaml
server:
  ports:
    - "9090:8080"   # 将宿主机 9090 端口映射到容器 8080
```

---

## 方式二：本地开发部署

适合开发调试场景，需本地安装所有依赖。

### 1. 安装依赖服务

确保本地已安装并启动：
- **MySQL 8.x**：默认端口 3306
- **Redis 7.x**：默认端口 6379

### 2. 初始化数据库

执行项目中的初始化 SQL 脚本：

```bash
mysql -u root -p < draw-guess-server/docker/init.sql
```

或手动在 MySQL 客户端中执行 `draw-guess-server/docker/init.sql` 的内容。

### 3. 修改开发环境配置

编辑 `draw-guess-server/src/main/resources/application-dev.yml`，根据本地实际情况修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/draw_guess?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true
    username: root
    password: root          # 修改为本地 MySQL 密码
  data:
    redis:
      host: localhost       # 修改为本地 Redis 地址
      port: 6379
```

### 4. 编译运行

```bash
cd draw-guess-server

# 编译
mvn clean compile

# 运行
mvn spring-boot:run
```

或先打包再运行：

```bash
# 打包
mvn clean package -DskipTests

# 运行 JAR
java -jar target/draw-guess-server-2.0.0.jar
```

### 5. 验证

访问 `http://localhost:8080`，应看到登录页面。

---

## 方式三：服务器生产部署

适合部署到云服务器（如阿里云、腾讯云、AWS 等）。

### 1. 准备服务器

- 操作系统：Linux（推荐 Ubuntu 22.04 / CentOS 8+）
- 最低配置：2 核 CPU / 2GB 内存 / 20GB 磁盘
- 安装 Docker 和 Docker Compose

```bash
# Ubuntu 安装 Docker
sudo apt update
sudo apt install docker.io docker-compose-plugin -y

# 验证
docker --version
docker compose version
```

### 2. 上传项目

```bash
# 方式一：Git 克隆
git clone <仓库地址>
cd DrawGuess_v2.0

# 方式二：SCP 上传
scp -r DrawGuess_v2.0 user@server:/opt/
```

### 3. 修改生产配置

编辑 `docker-compose.yml`，修改默认密码：

```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: <强密码>
    MYSQL_PASSWORD: <强密码>

server:
  environment:
    DB_PASSWORD: <与 MYSQL_PASSWORD 一致>
```

编辑 `draw-guess-server/src/main/resources/application.yml`，修改 JWT 密钥：

```yaml
jwt:
  secret: <自定义高强度密钥，建议32字符以上>
```

> **注意**：修改 JWT 密钥后需重新构建 Docker 镜像。

### 4. 启动服务

```bash
docker-compose up -d --build
```

### 5. 配置反向代理（可选）

如需域名访问或 HTTPS，推荐使用 Nginx 反向代理：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # WebSocket 支持
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

如需 HTTPS，可使用 Certbot 申请 Let's Encrypt 证书：

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

---

## 默认管理员账号

| 字段 | 值 |
|------|----|
| 手机号 | `00000000000` |
| 密码 | `admin123`（首次登录强制修改） |

---

## 常见问题

### Q: Docker 构建很慢怎么办？

Maven 首次下载依赖较慢，可在 `docker-compose.yml` 中为 builder 阶段配置国内镜像源，或使用本地 Maven 仓库缓存：

```yaml
server:
  build:
    context: ./draw-guess-server
    dockerfile: docker/Dockerfile
    args:
      MAVEN_REPO: https://maven.aliyun.com/repository/public
```

### Q: MySQL 容器启动失败？

- 检查 3306 端口是否被占用：`netstat -tlnp | grep 3306`
- 如端口冲突，修改 `docker-compose.yml` 中 MySQL 的端口映射：`"3307:3306"`

### Q: Redis 连接失败？

- 检查 Redis 容器是否正常运行：`docker-compose logs redis`
- 检查 6379 端口是否被占用

### Q: WebSocket 连接失败？

- 确保使用了 Nginx 反向代理时已配置 WebSocket 升级头（见上方 Nginx 配置）
- 前端 WebSocket 地址需与实际服务地址一致

### Q: 如何查看应用日志？

```bash
# Docker 部署
docker-compose logs -f server

# 本地部署
# 日志输出到控制台，或查看 Logback 配置的日志文件
```

### Q: 如何重置数据库？

```bash
# Docker 部署：删除数据卷并重启
docker-compose down -v
docker-compose up -d

# 本地部署：重新执行 init.sql
mysql -u root -p -e "DROP DATABASE draw_guess;"
mysql -u root -p < draw-guess-server/docker/init.sql
```

---

## 服务端口汇总

| 服务 | 容器内端口 | 默认映射端口 | 说明 |
|------|-----------|-------------|------|
| Spring Boot | 8080 | 8080 | HTTP + WebSocket |
| MySQL | 3306 | 3307 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |

---

## 技术架构概览

```
浏览器 ──HTTP/WS──> Spring Boot (8080)
                        ├── MySQL (3306)   持久化存储
                        └── Redis (6379)   缓存 + 在线状态
```

- **后端**：Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT
- **前端**：原生 HTML/CSS/JS（内嵌在 Spring Boot static 目录）
- **实时通信**：Spring WebSocket / STOMP
- **部署**：Docker 多阶段构建 + Docker Compose 编排
