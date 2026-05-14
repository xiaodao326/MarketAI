# 05 — 部署指南

> 状态: Phase 1 骨架 | 最后更新: 2026-05-14

## 架构

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Nginx:80 │ ←─ │ Backend  │ ←─ │  MySQL   │    │  Redis   │
│ (静态文件) │    │ :8080    │    │  :3306   │    │  :6379   │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
     ↑                ↑               ↑               ↑
     └────────────────┴───────────────┴───────────────┘
                    Docker Network (marketai-net)
```

## 前置要求

- **Docker** 20.10+ + **Docker Compose** 2.0+
- 至少 2GB 可用内存
- 至少 10GB 可用磁盘空间

## 生产部署步骤

### 1. 准备服务器

```bash
# 安装 Docker (Ubuntu 示例)
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER
newgrp docker
```

### 2. 配置环境变量

```bash
cd MarketAI

# 生成随机密钥
echo "JWT_SECRET=$(openssl rand -base64 48)" >> docker/.env
echo "MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24)" >> docker/.env

# 编辑 docker/.env 填写 AI API Key
vim docker/.env
```

必须填写的变量:
- `MYSQL_ROOT_PASSWORD` — MySQL root 密码
- `JWT_SECRET` — JWT 签名密钥 (\(\ge\) 32 字符)
- `DEEPSEEK_API_KEY` — AI API Key

### 3. 启动服务

```bash
# 构建镜像并启动
docker compose -f docker/docker-compose.yml up -d --build

# 查看日志
docker compose -f docker/docker-compose.yml logs -f

# 查看状态
docker compose -f docker/docker-compose.yml ps
```

### 4. 验证部署

```bash
# 检查后端健康
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},"aiProvider":{"status":"UP"}}}

# 检查前端
curl http://localhost:80/
# 返回 index.html

# 注册管理员账户
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@your-domain.com","password":"your-password","nickname":"Admin"}'
```

### 5. 配置反向代理 (可选)

Nginx 配置示例 (宿主机 Nginx 反代到 Docker):

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/your-domain.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.key;

    location / {
        proxy_pass http://127.0.0.1:80;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

## 数据备份

### MySQL

```bash
# 备份
docker exec marketai-mysql mysqldump -u root -p marketai > backup_$(date +%Y%m%d).sql

# 恢复
docker exec -i marketai-mysql mysql -u root -p marketai < backup_20260514.sql
```

### Redis

```bash
# Redis AOF 已默认开启, 备份 /data 卷即可
docker run --rm -v marketai_redis_data:/data -v $(pwd):/backup alpine cp -r /data /backup/redis_backup_$(date +%Y%m%d)
```

## 升级步骤

```bash
git pull
docker compose -f docker/docker-compose.yml up -d --build
# 检查新版是否正常
docker compose -f docker/docker-compose.yml logs backend | grep "Started"
```

## 环境变量参考

见 `docker/.env.example` 完整模板。

## 资源估算

| 环境 | CPU | 内存 | 磁盘 | 预期 QPS |
|------|-----|------|------|----------|
| 开发 | 2 核 | 4 GB | 20 GB | — |
| 生产 (小) | 2 核 | 4 GB | 40 GB | ~50 |
| 生产 (中) | 4 核 | 8 GB | 100 GB | ~200 |
| 生产 (大) | 8 核 | 16 GB | 500 GB | ~1000 |

## 日志管理

- 应用日志: `logback-spring.xml` 配置按天滚动, 保留 30 天
- Docker 日志: `docker compose logs` 查看, 建议配合 `docker logging driver` 限制大小
- 日志级别: 生产环境建议 `INFO`, 通过环境变量 `LOGGING_LEVEL_COM_MARKETAI=INFO` 覆盖