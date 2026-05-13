-- ============================================================
-- 增量迁移: 竞品分析模块 (2026-05-13)
-- 适用于已运行 init.sql 的数据库,补齐 competitors 新增列与 competitor_reports 表
--
-- 使用方式:
--   mysql -u root -p marketai < migrations/2026-05-13-competitor-analysis.sql
--
-- 幂等性: 列添加使用存储过程检测,新表使用 IF NOT EXISTS, 重复执行不会报错
-- ============================================================

-- ---- 1. competitors 表新增 3 列 ----------------------------------
DELIMITER //

DROP PROCEDURE IF EXISTS `add_column_if_not_exists` //
CREATE PROCEDURE `add_column_if_not_exists`(
    IN tbl VARCHAR(64),
    IN col VARCHAR(64),
    IN col_def TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', col_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL add_column_if_not_exists('competitors', 'strengths',  'JSON COMMENT ''AI 总结的优势列表''');
CALL add_column_if_not_exists('competitors', 'weaknesses', 'JSON COMMENT ''AI 总结的劣势列表''');
CALL add_column_if_not_exists('competitors', 'needs_user_input',
    'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''AI 不熟悉该竞品时为 1''');

DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;

-- ---- 2. 新建 competitor_reports 表 ------------------------------
CREATE TABLE IF NOT EXISTS `competitor_reports` (
    `id`                       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '报告主键',
    `project_id`               BIGINT      NOT NULL                COMMENT '所属项目 ID',
    `feature_matrix`           JSON                                COMMENT '{features:[...], opportunities:[...]}',
    `differentiation_insights` JSON                                COMMENT '差异化建议数组',
    `ai_model`                 VARCHAR(50) DEFAULT NULL            COMMENT '使用的 AI 模型',
    `tokens_used`              INT         DEFAULT 0               COMMENT '消耗的 token 数',
    `created_at`               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_id` (`project_id`),
    CONSTRAINT `fk_competitor_reports_project_id` FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞品对比分析报告表(项目级)';
