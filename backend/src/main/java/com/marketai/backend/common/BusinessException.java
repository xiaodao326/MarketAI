package com.marketai.backend.common;

import lombok.Getter;

/**
 * 业务异常 — 用于 service 层抛出可预见的错误
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
}