package com.marketai.backend.ai.llm.exception;

/**
 * 触发限流时抛出, 对应 HTTP 429
 * 来源: 用户端调用 RateLimiter 超限, 或 AI 提供商返回 429
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
