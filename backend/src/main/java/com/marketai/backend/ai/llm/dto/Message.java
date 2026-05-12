package com.marketai.backend.ai.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息, 对应 OpenAI / 各大模型的 messages[{role, content}] 格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /** system / user / assistant */
    private String role;

    private String content;

    public static Message system(String content) {
        return new Message("system", content);
    }

    public static Message user(String content) {
        return new Message("user", content);
    }

    public static Message assistant(String content) {
        return new Message("assistant", content);
    }
}
