# 03 — API 接口设计规范

> 状态: Phase 1 骨架 | 最后更新: 2026-05-14

## 基础约定

| 项目 | 约定 |
|------|------|
| 路径前缀 | `/api/v1/*` |
| 请求格式 | `Content-Type: application/json` |
| 认证方式 | `Authorization: Bearer <jwt_token>` |
| 日期格式 | `yyyy-MM-dd HH:mm:ss`, 时区 `Asia/Shanghai` |
| 文档访问 | `http://host:8080/swagger-ui.html` |

## 统一响应格式

```java
// com.marketai.backend.common.Result<T>
{
  "code": 200,        // 业务状态码 (int)
  "message": "success", // 状态描述 (String)
  "data": { }         // 业务数据 (T, 可能为 null)
}
```

## 错误码定义

### 标准 HTTP 状态码映射

| HTTP | code | message | 说明 |
|------|------|---------|------|
| 200 | 200 | success | 请求成功 |
| 400 | 400 | 参数错误 | 请求参数校验失败 |
| 401 | 401 | 未登录 | 缺少或无效 token |
| 403 | 403 | 无权限 | token 有效但权限不足 |
| 404 | 404 | 资源不存在 | 请求的资源未找到 |
| 409 | 409 | 资源冲突 | 邮箱已注册等 |
| 429 | 429 | 请求过于频繁 | AI 调用限流 |
| 500 | 500 | 服务器内部错误 | 未知异常 |

### 业务错误码 (ResultCode)

```java
// com.marketai.backend.common.ResultCode
SUCCESS(200, "success"),
PARAM_ERROR(400, "参数错误"),
UNAUTHORIZED(401, "未登录"),
FORBIDDEN(403, "无权限"),
NOT_FOUND(404, "资源不存在"),
CONFLICT(409, "资源冲突"),
TOO_MANY_REQUESTS(429, "请求过于频繁"),
SERVER_ERROR(500, "服务器内部错误"),

// 项目模块
PROJECT_NOT_FOUND(40401, "项目不存在"),
PROJECT_FORBIDDEN(40301, "无权操作此项目"),
```

错误码编码规则: `HTTP状态码 + 2位模块编号 + 2位具体错误`, 如 `40401` = 404(找不到) + 01(项目模块) + 01(不存在)。

## 接口列表 (Phase 1)

### 认证 (`/api/v1/auth`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/register` | 否 | 用户注册 |
| POST | `/login` | 否 | 用户登录 |
| POST | `/logout` | 是 | 退出登录 |
| GET | `/me` | 是 | 获取当前用户信息 |
| PUT | `/profile` | 是 | 更新个人信息 |

### 项目 (`/api/v1/projects`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/` | 是 | 创建项目 |
| GET | `/` | 是 | 项目列表 (分页) |
| GET | `/{id}` | 是 | 项目详情 |
| PUT | `/{id}` | 是 | 更新项目 |
| DELETE | `/{id}` | 是 | 归档项目 |
| POST | `/{id}/keywords` | 是 | 追加关键词 |
| DELETE | `/{id}/keywords/{keyword}` | 是 | 移除关键词 |

### 仪表盘 (`/api/v1/dashboard`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/{projectId}/metrics` | 是 | 核心指标卡 |
| GET | `/{projectId}/trend` | 是 | 时序趋势数据 |
| GET | `/{projectId}/keywords/top` | 是 | 热门关键词排行 |
| GET | `/{projectId}/anomalies` | 是 | 市场异动事件流 |

### AI 洞察 (`/api/v1/insights`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/` | 是 | 创建分析任务 (异步) |
| GET | `/tasks/{taskId}` | 是 | 查询任务状态 |
| GET | `/{reportId}` | 是 | 获取完整报告 |
| GET | `/project/{projectId}` | 是 | 项目历史报告列表 |

### 竞品分析 (`/api/v1/competitors`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/analyze` | 是 | 触发竞品分析 (异步) |
| GET | `/tasks/{taskId}` | 是 | 查询任务状态 |
| GET | `/project/{projectId}` | 是 | 项目下所有竞品 |
| GET | `/{id}` | 是 | 单个竞品详情 |
| PUT | `/{id}` | 是 | 更新竞品信息 |
| GET | `/project/{projectId}/matrix` | 是 | 功能对比矩阵 |
| GET | `/project/{projectId}/insights` | 是 | 差异化建议 |

### 用户画像 (`/api/v1/personas`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/generate` | 是 | 触发 AI 生成 (异步) |
| GET | `/tasks/{taskId}` | 是 | 查询任务状态 |
| GET | `/project/{projectId}` | 是 | 项目全部画像 |
| GET | `/{id}` | 是 | 单个画像 |
| POST | `/` | 是 | 手动创建画像 |
| PUT | `/{id}` | 是 | 更新画像 |
| DELETE | `/{id}` | 是 | 删除画像 |

### 数据采集 (`/api/v1/datasource`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/collect/{projectId}` | 是 | 手动触发采集 |
| GET | `/status` | 是 | 数据源健康状态 |

### LLM (`/api/v1/llm`)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/chat` | 是+管理员 | AI 对话测试 |
| GET | `/usage` | 是 | Token 消耗查询 |
| POST | `/switch` | 是+管理员 | 切换 LLM 提供商 |
| GET | `/providers` | 是 | 可用 Provider 列表 |

## 分页规范

```json
// 请求参数
{ "page": 1, "size": 12 }

// 响应格式
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 12,
    "pages": 9
  }
}
```

## 认证流程

```
1. POST /api/v1/auth/register  →  返回 { token, user }
2. POST /api/v1/auth/login     →  返回 { token, user }
3. 后续请求携带 Authorization: Bearer <token>
4. JwtAuthenticationFilter 解析 token → 注入 User 到 SecurityContext
5. Controller 通过 @AuthenticationPrincipal User user 获取当前用户
```
