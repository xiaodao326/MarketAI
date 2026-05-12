package com.marketai.backend.ai.llm.exception;

/**
 * API 账户余额不足时抛出, 对应 HTTP 402
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
