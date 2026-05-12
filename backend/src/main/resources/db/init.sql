-- ============================================================
-- MarketAI Phase 1 数据库初始化脚本
-- 版本: 1.0.0
-- 创建时间: 2026-05-11
-- 数据库: MySQL 8.0, 字符集 utf8mb4
-- ============================================================

-- 确保使用目标数据库
-- USE marketai;

-- ============================================================
-- 1. 用户表
-- 核心实体,存储注册用户信息,BCrypt 加密存储密码
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户主键',
    `email`         VARCHAR(100)    NOT NULL                 COMMENT '登录邮箱(唯一)',
    `password_hash` VARCHAR(255)    NOT NULL                 COMMENT 'BCrypt 加密后的密码',
    `nickname`      VARCHAR(50)     NOT NULL                 COMMENT '用户昵称',
    `avatar_url`    VARCHAR(500)    DEFAULT NULL             COMMENT '头像 URL',
    `status`        TINYINT         NOT NULL DEFAULT 1       COMMENT '账户状态: 0=禁用, 1=正常',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 分析项目表
-- 用户可创建多个分析项目,每个项目独立配置行业、关键词、竞品等信息
-- ============================================================
CREATE TABLE IF NOT EXISTS `projects` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '项目主键',
    `user_id`       BIGINT          NOT NULL                 COMMENT '所属用户 ID',
    `name`          VARCHAR(100)    NOT NULL                 COMMENT '项目名称',
    `description`   TEXT                                     COMMENT '项目描述',
    `industry`      VARCHAR(50)     NOT NULL                 COMMENT '行业标签, 如: 电商/SaaS/金融/教育',
    `target_market` VARCHAR(50)     NOT NULL                 COMMENT '目标市场区域, 如: 中国/北美/东南亚',
    `keywords`      JSON                                     COMMENT '监控关键词数组, 如: ["智能客服","AI客服"]',
    `competitors`   JSON                                     COMMENT '竞品名单数组, 如: ["Zendesk","Intercom"]',
    `status`        TINYINT         NOT NULL DEFAULT 0       COMMENT '项目状态: 0=草稿, 1=活跃, 2=归档',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_industry` (`industry`),
    CONSTRAINT `fk_projects_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析项目表';

-- ============================================================
-- 3. 市场趋势时序数据表
-- 存储各数据源的市场趋势指标,按日期粒度记录,支持多维度查询
-- ============================================================
CREATE TABLE IF NOT EXISTS `trend_data` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '数据主键',
    `project_id`    BIGINT          NOT NULL                 COMMENT '所属项目 ID',
    `keyword`       VARCHAR(100)    NOT NULL                 COMMENT '监控关键词',
    `source`        VARCHAR(20)     NOT NULL                 COMMENT '数据源: google_trends/baidu_index/weibo',
    `metric_type`   VARCHAR(20)     NOT NULL                 COMMENT '指标类型: search_volume/social_mentions/sentiment_score',
    `value`         DECIMAL(15,2)   NOT NULL                 COMMENT '指标数值',
    `data_date`     DATE            NOT NULL                 COMMENT '数据日期',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_keyword_date` (`project_id`, `keyword`, `data_date`),
    KEY `idx_source_date` (`source`, `data_date`),
    KEY `idx_metric_type` (`metric_type`),
    CONSTRAINT `fk_trend_data_project_id` FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='市场趋势时序数据表';

-- 防止重复采集: (项目, 关键词, 数据源, 指标类型, 日期) 五元组唯一
-- 配合 INSERT IGNORE 实现幂等写入 (continue-on-error: true 保证已存在索引时不报错)
CREATE UNIQUE INDEX IF NOT EXISTS `uk_trend_data`
    ON `trend_data` (`project_id`, `keyword`(50), `source`, `metric_type`, `data_date`);

-- ============================================================
-- 4. AI 需求洞察报告表
-- 存储 AI 模型生成的市场分析报告,包含多维度评分、痛点、机会、风险及行动建议
-- ============================================================
CREATE TABLE IF NOT EXISTS `insight_reports` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '报告主键',
    `project_id`        BIGINT          NOT NULL                 COMMENT '所属项目 ID',
    `title`             VARCHAR(200)    NOT NULL                 COMMENT '报告标题',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'pending' COMMENT '报告状态: pending/processing/completed/failed',
    `market_fit_score`  INT             DEFAULT NULL             COMMENT '市场需求匹配度评分 0-100',
    `dimensions`        JSON                                     COMMENT '维度评分: {demand_strength, competition_saturation, growth_potential, entry_barrier}',
    `pain_points`       JSON                                     COMMENT '痛点列表, 如: [{"title":"...","severity":"high","desc":"..."}]',
    `opportunities`     JSON                                     COMMENT '机会矩阵, 如: [{"area":"...","potential":85,"effort":60}]',
    `risks`             JSON                                     COMMENT '风险评估, 如: [{"risk":"...","probability":"high","impact":"medium","mitigation":"..."}]',
    `actions`           JSON                                     COMMENT '行动建议, 如: [{"priority":1,"action":"...","timeline":"短期","effort":"低"}]',
    `ai_model`          VARCHAR(50)     DEFAULT NULL             COMMENT '使用的 AI 模型: deepseek/qwen/glm',
    `tokens_used`       INT             DEFAULT 0                COMMENT '消耗的 token 数',
    `error_message`     TEXT            DEFAULT NULL             COMMENT '失败时的错误信息',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `completed_at`      DATETIME        DEFAULT NULL             COMMENT '报告生成完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`),
    KEY `idx_ai_model` (`ai_model`),
    CONSTRAINT `fk_insight_reports_project_id` FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 需求洞察报告表';

-- ============================================================
-- 5. 用户画像表
-- 基于 AI 分析生成的目标用户画像,包含决策参数和行为特征
-- ============================================================
CREATE TABLE IF NOT EXISTS `personas` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '画像主键',
    `project_id`        BIGINT          NOT NULL                 COMMENT '所属项目 ID',
    `name`              VARCHAR(50)     NOT NULL                 COMMENT '画像姓名/代号',
    `role`              VARCHAR(100)    NOT NULL                 COMMENT '职位/身份描述',
    `age_range`         VARCHAR(20)     NOT NULL                 COMMENT '年龄区间, 如: 25-35',
    `market_share`      DECIMAL(5,2)    NOT NULL DEFAULT 0.00    COMMENT '该画像在市场中的占比百分比',
    `is_primary`        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否核心用户: 0=否, 1=是',
    `goals`             JSON                                     COMMENT '核心目标标签数组, 如: ["提升效率","降低成本"]',
    `pain_points`       JSON                                     COMMENT '痛点列表, 如: [{"point":"...","severity":"high"}]',
    `quote`             TEXT                                     COMMENT '代表性用户引言',
    `decision_params`   JSON                                     COMMENT '决策参数: {payment_willingness, decision_cycle, budget_range, tech_capability}',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_is_primary` (`is_primary`),
    CONSTRAINT `fk_personas_project_id` FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像表';

-- ============================================================
-- 6. 竞品信息表
-- 存储竞品详细分析数据,包括功能矩阵、用户评分、融资阶段、威胁等级等
-- ============================================================
CREATE TABLE IF NOT EXISTS `competitors` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '竞品主键',
    `project_id`    BIGINT          NOT NULL                 COMMENT '所属项目 ID',
    `name`          VARCHAR(100)    NOT NULL                 COMMENT '竞品名称',
    `logo_url`      VARCHAR(500)    DEFAULT NULL             COMMENT '竞品 logo URL',
    `website`       VARCHAR(500)    DEFAULT NULL             COMMENT '竞品官网地址',
    `type`          VARCHAR(50)     NOT NULL                 COMMENT '竞品类型: direct/indirect/potential',
    `region`        VARCHAR(50)     NOT NULL                 COMMENT '竞品所在区域/国家',
    `rating`        DECIMAL(3,2)    NOT NULL DEFAULT 0.00    COMMENT '用户综合评分 0.00-5.00',
    `mau`           INT             DEFAULT NULL             COMMENT '月活跃用户数(估算)',
    `pricing_model` VARCHAR(200)    DEFAULT NULL             COMMENT '定价模式描述, 如: 订阅制/按量付费/免费增值',
    `funding_stage` VARCHAR(50)     DEFAULT NULL             COMMENT '融资阶段, 如: 种子轮/A轮/已上市',
    `threat_level`  VARCHAR(20)     NOT NULL DEFAULT 'medium' COMMENT '威胁等级: high/medium/low',
    `features`      JSON                                     COMMENT '功能矩阵: {"feature_name": "support_level"}(完全支持/部分支持/不支持)',
    `reviews`       JSON                                     COMMENT '用户评论摘要数组',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_type` (`type`),
    KEY `idx_threat_level` (`threat_level`),
    CONSTRAINT `fk_competitors_project_id` FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞品信息表';

-- ============================================================
-- 7. 异步分析任务表
-- 追踪 AI 分析任务的执行状态,result_id 多态关联到具体产出表
-- result_id 可能是 insight_reports/personas/competitors 中某张表的 ID,
-- 由 task_type 字段指明目标表,因此不建立外键约束
-- ============================================================
CREATE TABLE IF NOT EXISTS `analysis_tasks` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '任务主键',
    `user_id`           BIGINT          NOT NULL                 COMMENT '发起任务的用户 ID',
    `project_id`        BIGINT          NOT NULL                 COMMENT '所属项目 ID',
    `task_type`         VARCHAR(50)     NOT NULL                 COMMENT '任务类型: insight/persona/competitor',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'pending' COMMENT '任务状态: pending/running/completed/failed',
    `progress`          INT             NOT NULL DEFAULT 0       COMMENT '任务进度 0-100',
    `result_id`         BIGINT          DEFAULT NULL             COMMENT '关联产出表(insight_reports/personas/competitors)的主键 ID',
    `input_params`      JSON            NOT NULL                 COMMENT '任务输入参数, 如: {"keywords":["AI客服"],"competitors":["Zendesk"]}',
    `error_message`     TEXT            DEFAULT NULL             COMMENT '失败时的错误信息',
    `started_at`        DATETIME        DEFAULT NULL             COMMENT '任务开始执行时间',
    `completed_at`      DATETIME        DEFAULT NULL             COMMENT '任务完成时间',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_task_type_status` (`task_type`, `status`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_analysis_tasks_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_analysis_tasks_project_id` FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步分析任务表';
