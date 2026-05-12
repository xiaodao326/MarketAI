package com.marketai.backend.ai.llm;

import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;

/**
 * LLM 提供商统一接口 — 策略模式
 *
 * 选择策略模式的原因:
 *   1. 不同 AI 服务的 API 格式差异显著 (OpenAI 兼容 vs DashScope vs JWT 认证),
 *      策略模式让每个差异封装在独立类中, 主业务逻辑不受影响
 *   2. 切换模型无需修改调用方代码, 只需更改配置
 *   3. 新增模型只需新增一个 @Component 实现类, 遵循开闭原则
 */
public interface LLMProvider {

    /** 提供商唯一标识, 与 application.yml marketai.ai.provider 值对应 */
    String getName();

    /**
     * 普通对话
     * @throws com.marketai.backend.ai.llm.exception.TooManyRequestsException 触发限流 (429)
     * @throws com.marketai.backend.ai.llm.exception.InsufficientBalanceException 余额不足 (402)
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 结构化输出: 要求模型返回合法 JSON, 并反序列化为指定类型
     * 内部开启 JSON Mode, 解析失败抛出 RuntimeException
     */
    <T> T chatStructured(ChatRequest request, Class<T> responseType);

    /** 健康检查, 用于 GET /api/v1/datasource/status 等监控端点 */
    boolean healthCheck();
}
