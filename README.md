# MarketAI

AI 驱动的市场需求分析平台，面向创业者和企业产品/市场团队。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 (Composition API) + TypeScript + Vite + Tailwind CSS + Pinia |
| 后端 | Spring Boot 3.2 + Java 17 + MyBatis-Plus + Spring Security |
| 数据库 | MySQL 8.0 + Redis 7 |
| AI | DeepSeek API (默认) / 通义千问 / 智谱 GLM |
| 部署 | Docker + Docker Compose |

## 项目结构

```
marketai/
├── frontend/               # Vue 3 前端
│   ├── src/
│   │   ├── api/            # API 请求封装
│   │   ├── assets/         # 静态资源
│   │   ├── components/     # 通用组件
│   │   ├── composables/    # 组合式函数
│   │   ├── layouts/        # 布局组件
│   │   ├── router/         # 路由配置
│   │   ├── stores/         # Pinia 状态管理
│   │   ├── types/          # TypeScript 类型
│   │   ├── utils/          # 工具函数
│   │   └── views/          # 页面组件
│   └── package.json
├── backend/                # Spring Boot 后端
│   ├── src/main/java/com/marketai/backend/
│   │   ├── ai/             # AI 模型调用封装
│   │   ├── common/         # 通用类 (Result, 异常, 常量)
│   │   ├── config/         # 配置类
│   │   ├── controller/     # REST 控制器
│   │   ├── dto/            # 数据传输对象
│   │   ├── entity/         # 数据库实体
│   │   ├── mapper/         # MyBatis 接口
│   │   ├── security/       # 认证授权
│   │   ├── service/        # 业务逻辑
│   │   └── vo/             # 视图对象
│   └── pom.xml
├── docker/                 # Docker 配置
│   ├── docker-compose.yml
│   ├── Dockerfile.backend
│   └── Dockerfile.frontend
└── docs/                   # 文档
```

## 快速启动

### 方式一: Docker Compose (推荐)

```bash
# 1. 启动所有服务 (MySQL + Redis + 后端 + 前端)
cd docker
docker-compose up -d

# 2. 查看日志
docker-compose logs -f

# 3. 停止
docker-compose down
```

启动后访问:
- 前端: http://localhost:5173
- 后端 API: http://localhost:8080
- Swagger 文档: http://localhost:8080/swagger-ui.html

### 方式二: 本地开发

**前置要求:** Java 17+, Node.js 20+, Maven 3.9+, MySQL 8.0, Redis 7

```bash
# 1. 启动基础设施
cd docker
docker-compose up -d mysql redis

# 2. 启动后端
cd backend
mvn spring-boot:run

# 3. 启动前端 (新终端)
cd frontend
npm install
npm run dev
```

### 环境变量

复制 `docker/.env.example` 为 `docker/.env`，按需修改:

| 变量 | 说明 | 默认值 |
|------|------|--------|
| MYSQL_ROOT_PASSWORD | MySQL root 密码 | root123 |
| REDIS_PASSWORD | Redis 密码 | (空) |
| JWT_SECRET | JWT 签名密钥 | (开发默认值) |
| AI_PROVIDER | AI 服务商 | deepseek |
| AI_API_KEY | AI API Key | (需填写) |

## API 概览

所有 API 路径前缀: `/api/v1`

| 端点 | 说明 |
|------|------|
| POST /api/v1/auth/login | 用户登录 |
| (更多端点待开发) | |

统一响应格式:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 端口约定

| 服务 | 端口 |
|------|------|
| 前端 (Vite) | 5173 |
| 后端 (Spring Boot) | 8080 |
| MySQL | 3306 |
| Redis | 6379 |