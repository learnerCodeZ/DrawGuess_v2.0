# 阶段二：用户系统完善 — 总结

> 完成时间：2026-06-26
> 对应 PRD 章节：第 8 章 - 阶段二

---

## 一、本阶段完成的工作

阶段二的核心目标：**将阶段一的骨架代码从"能编译"提升到"能用"**，重点修复安全流程和 API 设计缺陷。

### 1. 登录流程重构

**问题**：阶段一的登录逻辑在 Service 层直接查库+比对密码，没有走 Spring Security 的 `AuthenticationManager`，绕过了安全框架。

**改进**：虽然登录接口是自定义的（不使用 Security 默认的 formLogin），但保留了 Service 层的登录逻辑，因为：
- 登录响应需要返回自定义的 `ApiResponse` 格式（含 token + user info）
- 需要在登录时做额外业务判断（审核状态、首次登录强制改密）
- Spring Security 的 `AuthenticationManager` 更适合标准表单登录场景

**最终登录流程**：
```
POST /api/login { phone, password }
    → UserService.login()
        → 查用户 → 校验审核状态 → 校验密码
        → 超级管理员默认密码? → 返回临时 token (code=1008)
        → 正常登录 → 返回正式 JWT token (code=200)
```

### 2. 超级管理员首次登录强制改密

这是阶段二最重要的修复。阶段一的实现有严重缺陷：

| 问题 | 阶段一 | 阶段二 |
|------|--------|--------|
| 改密认证 | 无认证，任何人可改他人密码 | 使用临时 JWT token（5分钟有效） |
| token 类型 | 无区分 | `isTemp` claim 区分正式/临时 |
| token 权限 | 无限制 | 临时 token 只能访问 `/api/change-password` |
| 改密后响应 | 无 | 返回新的正式 token + 用户信息 |

**完整流程**：
```
1. POST /api/login { phone: "00000000000", password: "admin123" }
   → 响应: { code: 1008, message: "请先修改默认密码", data: { tempToken: "xxx" } }

2. POST /api/change-password { tempToken: "xxx", newPassword: "newPass123" }
   → JwtTokenProvider 验证临时 token → 提取 userId → 修改密码
   → 响应: { code: 200, data: { token: "正式token", user: {...} } }

3. 后续请求使用正式 token
```

### 3. Security 异常响应 JSON 化

**问题**：Spring Security 默认对未认证请求返回 302 重定向到登录页，对权限不足返回 HTML 错误页。前后端分离架构下，前端需要 JSON 格式。

**新增两个组件**：

| 类 | 处理场景 | 响应格式 |
|----|---------|---------|
| `JwtAuthenticationEntryPoint` | 未认证访问受保护资源 | `{ code: 401, message: "未登录" }` |
| `JwtAccessDeniedHandler` | 权限不足（普通用户访问管理员接口） | `{ code: 403, message: "权限不足" }` |

在 `SecurityConfig` 中通过 `.exceptionHandling()` 注入。

### 4. GlobalExceptionHandler 增强

新增 JWT 异常处理：

| 异常类型 | 场景 | 响应码 |
|---------|------|--------|
| `ExpiredJwtException` | Token 过期 | 9003 |
| `JwtException` | Token 无效/篡改 | 9002 |

### 5. 管理员 API 完善

| 改进项 | 说明 |
|--------|------|
| 新增 `POST /api/admin/reject` | PRD 功能需求中"拒绝注册申请"在 API 列表中缺失，补充 |
| 新增 `GET /api/admin/users/pending` | 查看待审核用户列表 |
| 新增 `GET /api/admin/users/rejected` | 查看待被拒绝用户列表 |
| 所有用户列表接口返回 `UserInfoVO` | 不暴露 password 等敏感字段 |

### 6. 其他修复

- `ChangePasswordRequest` 增加 `@NotBlank` 校验注解
- `ApiResponse` 新增 `error(code, message, data)` 工厂方法，支持错误时携带数据
- `UserService` 接口新增 `forceChangePassword` 方法

---

## 二、新增/修改文件清单

### 新增文件 (2)
- `security/JwtAuthenticationEntryPoint.java` — 未认证 JSON 响应
- `security/JwtAccessDeniedHandler.java` — 权限不足 JSON 响应

### 修改文件 (9)
- `security/JwtTokenProvider.java` — 新增临时 token 生成/判断
- `security/JwtAuthenticationFilter.java` — 临时 token 路径限制
- `config/SecurityConfig.java` — 注入异常处理器
- `common/ApiResponse.java` — 新增 errorWithData 方法
- `common/GlobalExceptionHandler.java` — 新增 JWT 异常处理
- `controller/UserController.java` — 登录/改密流程重写
- `controller/AdminController.java` — 增加 reject/pending/rejected 端点
- `service/UserService.java` — 新增 forceChangePassword
- `service/AdminService.java` + `impl` — 新增 getRejectedUsers
- `model/dto/ChangePasswordRequest.java` — 增加 @NotBlank

---

## 三、核心架构决策

### 3.1 临时 Token vs 记住密码标记

**方案 A（已选）**：生成临时 JWT token，5 分钟有效，包含 `temp=true` claim
**方案 B（备选）**：在用户表增加 `must_change_password` 字段

选 A 的原因：
- 无状态，不增加数据库字段
- 时间限制精确（5 分钟 vs 依赖下次请求检查）
- Filter 层即可拦截，不需要每个接口检查

### 3.2 登录异常 vs 正常返回

超级管理员首次登录属于"异常"还是"正常"？

**决策**：用异常抛出，Controller 层 catch 转为特殊响应格式。原因：
- 从业务角度看，"需要改密"确实是一种登录失败（没有拿到正式 token）
- 用异常可以统一走 ResultCode 体系
- Controller 层的特殊处理是必要的，因为需要携带 tempToken 数据

### 3.3 管理员接口返回 VO vs Entity

**决策**：统一返回 `UserInfoVO`，不暴露原始 `User` 实体
**原因**：
- `User` 实体包含 `password` 字段，即使 BCrypt 加密也不应返回
- VO 是 API 契约，Entity 是数据库映射，两者应解耦
- 前端不需要 `password`、`updatedAt` 等字段

---

## 四、API 清单（用户系统相关）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/register | 无需 | 注册 |
| POST | /api/login | 无需 | 登录 |
| POST | /api/change-password | 临时token | 超管首次改密 |
| GET | /api/user | JWT | 获取当前用户 |
| PUT | /api/user/nickname | JWT | 修改昵称 |
| PUT | /api/user/password | JWT | 修改密码 |
| DELETE | /api/user | JWT | 注销账户 |
| GET | /api/admin/users | 管理员 | 所有用户列表 |
| GET | /api/admin/users/pending | 管理员 | 待审核用户 |
| GET | /api/admin/users/rejected | 管理员 | 被拒绝用户 |
| POST | /api/admin/approve | 管理员 | 审核通过 |
| POST | /api/admin/reject | 管理员 | 审核拒绝 |
| DELETE | /api/admin/users/{userId} | 管理员 | 注销用户 |

---

## 五、待后续阶段完善

| 项目 | 计划阶段 |
|------|---------|
| Redis 缓存 Token 黑名单（注销/改密后旧 token 失效） | 阶段三 |
| 记住登录状态（Token 续期） | 阶段三 |
| 用户在线状态（Redis Set） | 阶段三 |
| 前端页面联调 | 阶段七 |
