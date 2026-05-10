package com.marketai.backend.common;

/**
 * 项目级常量定义
 */
public final class Constants {

    private Constants() {}

    /** 请求头中的 token key */
    public static final String TOKEN_HEADER = "Authorization";

    /** token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Redis token 前缀 */
    public static final String REDIS_TOKEN_PREFIX = "marketai:token:";

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;
}