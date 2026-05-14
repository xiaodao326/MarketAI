# ==================================
# MarketAI Backend — 生产多阶段构建
# 阶段1: Maven 编译打包
# 阶段2: JRE 运行 (最小镜像)
# ==================================

# ── 阶段1: 构建 ──────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 先复制 pom.xml, 利用 Docker 缓存下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# 复制源码并打包
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── 阶段2: 运行 ──────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

RUN addgroup -S marketai && adduser -S marketai -G marketai
USER marketai

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
