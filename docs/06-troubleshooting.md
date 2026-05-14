# 06 — 常见问题与排查

> 状态: Phase 1 骨架 | 最后更新: 2026-05-14

## 后端启动问题

### 数据库连接失败

**现象**: 启动报 `Communications link failure` 或 `Access denied for user`

**排查**:
```bash
# 确认 MySQL 容器已启动且健康
docker compose -f docker/docker-compose.dev.yml ps
# health 列应为 "healthy"

# 测试连接
docker exec marketai-mysql-dev mysqladmin ping -h localhost
```

**解决**:
- 检查 `application.yml` 中 `spring.datasource.url` 的 host 是否正确
- 本地开发用 `localhost`, Docker 内用 `mysql` (容器名)
- 确认 `MYSQL_PASSWORD` 环境变量已设置

### Redis 连接失败

**现象**: 启动报 `Unable to connect to Redis`

**解决**:
- Docker 开发: `docker compose -f docker/docker-compose.dev.yml up -d redis`
- 检查 `REDIS_PASSWORD` 环境变量是否与 Redis 配置一致

### JWT 签名无效

**现象**: 请求返回 401, 日志显示 `JWT signature does not match`

**解决**: 确保 `JWT_SECRET` 在所有实例间一致, 生成至少 32 个字符的随机密钥

### AI API 调用失败

**现象**: AI 分析任务状态为 `failed`, `error_message` 显示 401/403

**排查**:
```bash
# 检查配置的 API Key
grep "DEEPSEEK_API_KEY" docker/.env
# 测试 API 连通性
curl -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
  https://api.deepseek.com/v1/models
```

**解决**: 确保 `DEEPSEEK_API_KEY` 有效, 账户余额充足

## 前端问题

### 页面刷新 404

**现象**: 非根路径刷新后显示 nginx 404

**解决**: 确保 nginx 配置中 `try_files $uri $uri/ /index.html` 正确设置 (SPA 路由回退)

### API 请求 401

**现象**: 登录成功后接口返回 401

**排查**:
- 检查 axios 拦截器是否在请求头中携带 `Authorization: Bearer <token>`
- 检查 token 是否过期 (默认 7 天)

### Vite 代理不工作

**现象**: 前端 `npm run dev` 后 API 请求 404

**解决**: 确保 `vite.config.ts` 中 proxy 配置正确:
```ts
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}
```

## Docker 问题

### 端口冲突

**现象**: `Error starting userland proxy: Bind for 0.0.0.0:3306 failed: port is already allocated`

**解决**:
```bash
# 查看占用端口的进程
netstat -ano | findstr 3306  # Windows
lsof -i :3306               # Linux/Mac
```

### 构建缓慢

**解决**:
- 后端: 首次构建 `mvn dependency:go-offline` 耗时较长, 后续利用 Docker 缓存
- 前端: `npm ci` 较慢时可用 `npm install`, 或配置 npm 镜像源

### 数据卷清理

```bash
# 删除所有数据卷 (重置数据库)
docker compose -f docker/docker-compose.yml down -v
```

## 性能问题

### 仪表盘查询慢

**排查**:
```sql
-- 检查趋势数据表索引是否生效
EXPLAIN SELECT * FROM trend_data
WHERE project_id = 1 AND source = 'baidu_index'
AND metric_type = 'search_volume'
AND data_date BETWEEN '2026-05-01' AND '2026-05-07';
```

**优化**: 确保 `idx_trend_project_source_metric_date` 索引存在且被使用 (type=range, 非 ALL)

### AI 分析超时

**解决**:
- 调整 `ChatRequest.maxTokens` (默认 4000, 减少可加快响应)
- 检查 DeepSeek API 当前负载状态
- 确认 `marketai.ai.rate-limit.requests-per-minute` 未限制当前任务

## 获取帮助

1. 查看应用日志: `docker compose logs -f backend`
2. 检查健康端点: `GET /actuator/health`
3. 提交 Issue 时请附上:
   - 错误日志 (脱敏后)
   - 环境信息 (`docker version`, `java -version`, `node -v`)
   - 重现步骤
