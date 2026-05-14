package com.marketai.backend.ai.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "对话消息, 对应 OpenAI messages 格式")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Schema(description = "角色: system/user/assistant", example = "user")
    private String role;

    @Schema(description = "消息内容", example = "你好")
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
