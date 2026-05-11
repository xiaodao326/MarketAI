-- H2 测试 Schema (MySQL 兼容模式)
-- 仅包含 UserService 测试所需的 users 表

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    email         VARCHAR(100)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    nickname      VARCHAR(50)     NOT NULL,
    avatar_url    VARCHAR(500)    DEFAULT NULL,
    status        TINYINT         NOT NULL DEFAULT 1,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email)
);
