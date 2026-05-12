package com.marketai.backend.ai.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话请求
 * 使用 @NoArgsConstructor + 字段默认值, Jackson 反序列化时缺省字段自动取默认值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private List<Message> messages = new ArrayList<>();

    /** 生成随机性, 0~1, 默认 0.7 */
    private Double temperature = 0.7;

    /** 最大输出 token 数, 默认 4000 */
    private Integer maxTokens = 4000;

    /** 是否开启 JSON Mode (要求模型输出合法 JSON) */
    private Boolean jsonMode = false;

    /** JSON Schema 提示 (可选), 告知模型期望的 JSON 结构 */
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
