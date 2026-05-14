# ==================================
# MarketAI Frontend — 生产多阶段构建
# 阶段1: Node 编译 Vite 项目
# 阶段2: Nginx 提供静态文件
# ==================================

# ── 阶段1: 构建 ──────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /build

# 先复制依赖文件, 利用 Docker 缓存
COPY package.json package-lock.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ── 阶段2: 运行 ──────────────────────────────────
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]