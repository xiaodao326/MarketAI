package com.marketai.backend.ai;

/**
 * AI 模型调用抽象 — 支持 DeepSeek/通义千问/智谱 GLM 切换
 */
public interface AiProvider {

    /**
     * 向 AI 模型发送对话请求
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return AI 回复文本
     */
    String chat(String systemPrompt, String userMessage);
}