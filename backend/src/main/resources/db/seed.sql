-- ============================================================
-- MarketAI Phase 1 种子数据脚本
-- 用途: 本地开发和测试使用
-- 密码: demo1234 (BCrypt 加密)
-- ============================================================

-- 测试用户
INSERT INTO `users` (`id`, `email`, `password_hash`, `nickname`, `status`)
VALUES (1, 'demo@marketai.com', '$2b$10$paLrVfvXXhc.fA/CKoFDF.gOdesdEK7sXsi6.SJ1JkSu0Yzl5BbcC', 'Demo用户', 1);

-- 示例分析项目: AI 智能客服市场分析
INSERT INTO `projects` (`id`, `user_id`, `name`, `description`, `industry`, `target_market`, `keywords`, `competitors`, `status`)
VALUES (
    1,
    1,
    'AI 智能客服市场机会分析',
    '分析企业级 AI 智能客服市场,评估进入机会和核心用户群体需求痛点',
    'SaaS',
    '中国',
    '["AI客服","智能客服","大模型客服","对话机器人","客服自动化"]',
    '["Zendesk","Intercom","智齿科技","网易七鱼","美洽","环信"]',
    1
);
