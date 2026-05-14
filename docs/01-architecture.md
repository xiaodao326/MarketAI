# 01 — 整体架构与模块划分

> 状态: Phase 1 骨架 | 最后更新: 2026-05-14

## 架构分层

```
┌─────────────────────────────────────────────┐
│                Nginx (SPA + 反向代理)          │
├─────────────────────────────────────────────┤
│          Vue 3 SPA (Vite 构建产物)             │
├─────────────────────────────────────────────┤
│        Spring Boot REST API (8080)           │
│  ┌──────────┬───────────┬─────────────────┐ │
│  │ Security │ Controller│   AI 模块        │ │
│  │  (JWT)   │   层       │  (LLM/洞察/画像)  │ │
│  ├──────────┼───────────┼─────────────────┤ │
│  │ Service  │  Common   │   数据采集模块     │ │
│  │   层      │  (Result) │  (百度/微博)      │ │
│  ├──────────┴───────────┴─────────────────┤ │
│  │         MyBatis-Plus (ORM)             │ │
│  └────────────────────────────────────────┘ │
├─────────────────────────────────────────────┤
│           MySQL 8.0    │    Redis 7          │
└─────────────────────────────────────────────┘
```

## 模块划分

### 前端 (`frontend/`)

| 模块 | 路径 | 职责 |
|------|------|------|
| 路由 | `router/` | Vue Router, 守卫与懒加载 |
| 状态 | `stores/` | Pinia stores (用户信息, 项目) |
| API 封装 | `api/` | axios 实例 + 拦截器 |
| 组合函数 | `composables/` | 可复用逻辑 |
| 组件 | `components/` | 通用 UI 组件 |
| 视图 | `views/` | 页面级组件 |

### 后端 (`backend/`)

| 模块 | 包名 | 职责 |
|------|------|------|
| AI 调用 | `ai.llm` | LLM Provider 抽象 + DeepSeek/千问/GLM 实现 |
| 需求洞察 | `ai.insight` | AI 生成市场分析报告, 异步任务队列 |
| 用户画像 | `ai.persona` | AI 生成目标用户画像 |
| 竞品分析 | `ai.competitor` | AI 生成竞品对比矩阵与差异化建议 |
| 数据采集 | `ai.datasource` | 百度指数/微博热搜等外部数据源采集 |
| 认证 | `security` | JWT 生成/验证/刷新 |
| 通用 | `common` | Result 封装, BusinessException, 错误码 |
| 配置 | `config` | Security, CORS, OpenAPI, Redis, MyBatis-Plus |

## 数据流

```
用户操作 → Vue 组件 → Pinia Store → axios → Spring Controller
    → Service (业务逻辑)
        → MyBatis Mapper → MySQL (读写)
        → Redis (缓存/任务队列)
        → LLM Provider API (AI 分析)
    → Result<T> 统一响应
→ axios 拦截器解包 → Pinia Store 更新 → Vue 响应式渲染
```

## 异步任务流

AI 分析任务 (洞察/画像/竞品) 采用异步模式:

```
POST /api/v1/xxx → 创建 AnalysisTask (status=pending) → 立即返回 taskId
     ↓
TaskConsumer (轮询 pending 任务) → status=running
     ↓
调用 LLM Provider → 解析 JSON → 写入报告表
     ↓
更新 task status=completed, result_id=报告ID
     ↓
前端轮询 GET /tasks/{taskId} → 获取 result_id → 拉取报告
```

## 技术决策

- **Spring Boot 3.2 + JDK 21**: 虚拟线程已 GA, 但当前异步任务走 `@EnableScheduling` 定时轮询, Phase 2 可迁移至虚拟线程
- **MyBatis-Plus vs JPA**: 选择 MP 因为更灵活的原生 SQL 支持, JSON 列类型处理器完善
- **JSON 列**: keywords/competitors/features 等使用 MySQL JSON 类型, 配合 MP 的 JacksonTypeHandler
- **Redis**: 当前用于缓存 + 任务队列, Phase 2 可增加 Session 共享 + 限流计数