package com.marketai.backend.ai.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "AI 对话请求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @Schema(description = "对话消息列表")
    private List<Message> messages = new ArrayList<>();

    @Schema(description = "生成随机性, 0~1", example = "0.7")
    private Double temperature = 0.7;

    @Schema(description = "最大输出 token 数", example = "4000")
    private Integer maxTokens = 4000;

    @Schema(description = "是否开启 JSON Mode")
    private Boolean jsonMode = false;

    @Schema(description = "JSON Schema 提示 (可选), 告知模型期望的 JSON 结构")
    private String responseSchema;

    /** 便捷构造: 单条 user 消息 */
    public static ChatRequest of(String userContent) {
        ChatRequest req = new ChatRequest();
        req.setMessages(List.of(Message.user(userContent)));
        return req;
    }

    /** 便捷构造: system + user 消息 */
    public static ChatRequest of(String systemContent, String userContent) {
        ChatRequest req = new ChatRequest();
        req.setMessages(List.of(Message.system(systemContent), Message.user(userContent)));
        return req;
    }
}
