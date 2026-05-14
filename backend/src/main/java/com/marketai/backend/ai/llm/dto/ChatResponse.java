package com.marketai.backend.ai.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "AI 对话响应")
@Data
@Builder
public class ChatResponse {

    @Schema(description = "模型生成的文本内容")
    private String content;

    @Schema(description = "输入 token 数")
    private Integer promptTokens;

    @Schema(description = "输出 token 数")
    private Integer completionTokens;

    @Schema(description = "总 token 数")
    private Integer totalTokens;

    @Schema(description = "实际使用的模型名称")
    private String model;

    @Schema(description = "请求 ID, 用于排查问题")
    private String requestId;

    @Schema(description = "调用耗时 (ms)")
    private Long latencyMs;
}
