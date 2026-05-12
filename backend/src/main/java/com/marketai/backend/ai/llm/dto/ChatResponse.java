package com.marketai.backend.ai.llm.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 对话响应, 统一封装各 Provider 的返回结果
 */
@Data
@Builder
public class ChatResponse {

    /** 模型生成的文本内容 */
    private String content;

    /** 输入 token 数 */
    private Integer promptTokens;

    /** 输出 token 数 */
    private Integer completionTokens;

    /** 总 token 数 */
    private Integer totalTokens;

    /** 实际使用的模型名称 */
    private String model;

    /** 请求 ID, 用于排查问题 */
    private String requestId;

    /** 调用耗时 (ms) */
    private Long latencyMs;
}
