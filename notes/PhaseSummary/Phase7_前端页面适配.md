# 阶段七：前端页面适配 — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段七

---

## 一、本阶段完成的工作

创建 4 个完整的前端静态页面 + CSS + JS，组成完整的单页应用（SPA 风格）。

### 1. 文件清单

```
static/
├── css/
│   └── style.css          ← 全局样式（~300行）
├── js/
│   └── api.js             ← API 封装 + 工具函数（~100行）
├── index.html              ← 登录/注册页
├── home.html               ← 首页（好友+房间+排行榜）
├── room.html               ← 游戏房间（画布+答题+STOMP）
└── admin.html              ← 管理后台
```

### 2. 页面功能对照

#### index.html — 登录/注册

| 功能 | 实现方式 |
|------|---------|
| 登录 | 表单 → POST /api/login → 存 token+user → 跳转 |
| 注册 | 表单 → POST /api/register → 切换回登录页 |
| 超管强制改密 | 登录报错 1008 → 弹出改密弹窗 → 临时 token → 改密成功 → 跳转 |
| 输入校验 | 前端：手机号 11 位、密码 ≥ 6 位；后端 @Valid 兜底 |
| 已登录检测 | 页面加载时检查 localStorage token |

#### home.html — 首页

| 模块 | 功能 | API |
|------|------|-----|
| 房间列表 | 显示在线房间（房主、人数、状态） | GET /api/admin/rooms |
| 创建房间 | 一键创建并跳转 | POST /api/rooms |
| 加入房间 | 输入房间号跳转 | URL 参数传递 |
| 好友列表 | 显示好友+在线状态 | GET /api/friends |
| 搜索添加 | 搜索用户+发送好友请求 + 请求通知 | GET search + POST request |
| 好友请求 | 同意/拒绝 | POST accept/reject |
| 排行榜 | Top 100 带奖牌 | GET /api/leaderboard |

#### room.html — 游戏房间（核心页面）

| 模块 | 功能 | 技术 |
|------|------|------|
| Canvas 画布 | 鼠标/触摸绘画 + 多色 + 粗细 + 清空 + 撤销 | HTML5 Canvas |
| 实时绘制 | 绘制数据通过 STOMP 实时广播 | WebSocket `/app/draw` |
| 游戏信息 | 房间号、回合数、倒计时、词语提示 | tick 事件驱动 |
| 成员列表 | 显示成员（房主/画家标记）、分数 | WebSocket room_state |
| 答题区 | 非画家可见，输入答案并发送 | `/app/submit_answer` |
| 游戏控制 | 房主可见"开始游戏"按钮 | `/app/start_game` |
| 结果弹窗 | 终局排名（奖牌+昵称+分数） | game_end 事件 |
| 倒计时 | 30秒倒计时，≤10秒变为红色闪烁 | tick 事件 |
| 画布水印 | 等待中/游戏中/画家提示 | 状态驱动 |

**核心交互流程**：
```
1. 连接 WebSocket /ws?token=xxx
2. 发送 /app/join_room → 收到 room_state → 渲染成员列表
3. 房主点击"开始" → 收到 game_start + round_start
4. 画家收到 round_word（私信）+ 显示工具栏 → 开始画画
5. 非画家看到 ______（字数提示）→ 输入答案
6. 猜对/错 → 收到 answer_correct/answer_result
7. 每秒 tick → 更新倒计时
8. 回合结束 → 自动下一轮或 game_end
```

#### admin.html — 管理后台

| 模块 | 功能 |
|------|------|
| 统计仪表盘 | 7 项指标卡片：总用户、待审核、活跃房间等 |
| 用户管理 | 全部/待审核/已拒绝 三种视图，通过/拒绝/注销操作 |
| 词库管理 | 列表显示、单个添加、批量添加、删除（默认词不可删） |
| 房间管理 | 暂停 → 恢复 → 解散完整工作流 |

### 3. 样式设计

| 项目 | 方案 |
|------|------|
| UI 框架 | 纯原生 CSS，无外部依赖 |
| 设计风格 | Ant Design 风格（白色卡片、蓝色主色、圆角） |
| 图标 | Font Awesome 6 |
| 响应式 | Canvas 区 ≥ 500px，≤ 900px 时垂直堆叠 |
| 弹窗/提示 | 自定义模态框 + Toast 消息 |
| 动画 | tick 倒计时 ≤10s 红色脉冲动画 |

### 4. WebSocket 前端集成

使用 `sockjs-client` + `stomp.js`（CDN 引入）：

```javascript
// 连接
const socket = new SockJS('/ws?token=' + token);
const client = Stomp.over(socket);

// 订阅房间广播
client.subscribe('/topic/room/' + roomId, callback);

// 订阅私信（画家的词语）
client.subscribe('/user/queue/word', callback);

// 发送消息
client.send('/app/submit_answer', {}, JSON.stringify({ roomId, answer }));
```

---

## 二、新增/修改文件清单

### 新增文件 (6)
- `static/css/style.css` — 全局样式（~300 行）
- `static/js/api.js` — API 封装 + 工具函数
- `static/index.html` — 登录/注册页
- `static/home.html` — 首页
- `static/room.html` — 游戏房间（画布+STOMP）
- `static/admin.html` — 管理后台

---

## 三、核心架构决策

### 3.1 纯原生前端 vs Vue/React

**决策**：原生 HTML + CSS + JS。

**原因**：
- PRD 明确指定"原生 HTML + CSS + JS（保留原版 UI）"
- 项目无需构建工具，Maven 直接 serve 即可
- 减少了依赖体积和学习成本

**权衡**：
- 缺少组件化（代码组织不如框架清晰）
- 前端状态管理手动（localStorage + 全局变量）
- 适合 LAN 游戏场景，不适合大型前端应用

### 3.2 Canvas 绘制：实时广播 vs 本地+同步

**决策**：每次绘制（mousemove）都广播，其他人收到后立即绘制。

**问题**：mousemove 触发太频繁，广播量可能很大。

**当前方案**：
- 每帧广播一次绘制数据（mousedown 时 saveState 用于撤销）
- 没有服务端存储画布状态
- 后加入的玩家看不到之前的绘制

**改进空间**：如需优化，可以用：
1. 节流（throttle）：每 50ms 聚合一次绘制点
2. 服务端缓存画布状态（Redis）
3. 只传输 points 数组，而非每帧坐标

### 3.3 SockJS 降级

**决策**：使用 SockJS（`withSockJS()`）。

**原因**：
- 浏览器不支持 WebSocket 时自动降级为 XHR 轮询或 JSONP 轮询
- Spring WebSocket 对 SockJS 有原生支持
- 局域网环境 WebSocket 成功率极高，降级作为兜底

---

## 四、前端 API 对照（全部 24+ 个 REST API）

| API | 前端用法 | 页面 |
|-----|---------|------|
| POST /api/register | 注册表单 | index.html |
| POST /api/login | 登录表单 | index.html |
| POST /api/change-password | 超管改密弹窗 | index.html |
| GET /api/user | 页面加载时从 localStorage 获取 | 通用 |
| PUT /api/user/nickname | 待个人中心 | 后续 |
| PUT /api/user/password | 待个人中心 | 后续 |
| GET /api/friends/search | 搜索用户弹窗 | home.html |
| POST /api/friends/request | 添加好友按钮 | home.html |
| GET /api/friends/requests | 好友请求弹窗 | home.html |
| POST /api/friends/accept | 同意按钮 | home.html |
| POST /api/friends/reject | 拒绝按钮 | home.html |
| GET /api/friends | 好友列表模块 | home.html |
| DELETE /api/friends/{id} | 删除按钮 | home.html |
| POST /api/rooms | 创建房间按钮 | home.html |
| GET /api/rooms/{roomId} | 页面初始化 | room.html |
| POST /api/rooms/{roomId}/join | WebSocket join_room | room.html |
| GET /api/leaderboard | 排行榜模块 | home.html |
| GET /api/admin/users/* | 用户管理 | admin.html |
| POST /api/admin/approve/reject | 按钮点击 | admin.html |
| GET /api/admin/words | 词库列表 | admin.html |
| POST /api/admin/words (batch) | 添加/批量添加 | admin.html |
| GET /api/admin/rooms | 房间管理 | admin.html + home.html |
| POST /api/admin/rooms/{id}/* | 暂停/恢复/解散 | admin.html |
| GET /api/admin/stats | 统计卡片 | admin.html |

---

## 五、待后续完善

| 项目 | 说明 |
|------|------|
| 个人中心页 | 修改昵称、密码（当前可通过 API 完成，无 UI） |
| 游戏记录查看 | 已有 API 但前端无入口 |
| 画布撤销优化 | 服务端缓存历史（P1 功能） |
| 橡皮擦工具 | P2 功能，待用户需求 |
| 用户注销 | 已有 API 但前端无 UI |
