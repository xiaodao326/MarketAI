package com.marketai.backend.ai.llm.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.llm.LLMProvider;
import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;
import com.marketai.backend.ai.llm.exception.InsufficientBalanceException;
import com.marketai.backend.ai.llm.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek 提供商
 * API 兼容 OpenAI 格式: POST {baseUrl}/chat/completions
 * 模型: deepseek-chat (通用) / deepseek-reasoner (深度推理)
 */
@Slf4j
@Component
public class DeepSeekProvider implements LLMProvider {

    private static final int MAX_RETRIES = 3;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final long retryDelayMs; // 允许测试时注入更短的延迟

    /** Spring 注入入口 */
    @Autowired
    public DeepSeekProvider(
            @Value("${marketai.ai.deepseek.base-url:https://api.deepseek.com/v1}") String baseUrl,
            @Value("${marketai.ai.deepseek.api-key:}") String apiKey,
            @Value("${marketai.ai.deepseek.model:deepseek-chat}") String model,
            ObjectMapper objectMapper) {
        this(baseUrl, apiKey, model, buildClient(30), objectMapper, 1000L);
    }

    /** 测试专用构造器: 可注入自定义 OkHttpClient 和重试延迟 */
    DeepSeekProvider(String baseUrl, String apiKey, String model,
                     OkHttpClient client, ObjectMapper objectMapper, long retryDelayMs) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = client;
        this.objectMapper = objectMapper;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public String getName() {
        return "deepseek";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return executeWithRetry(request);
    }

    @Override
    public <T> T chatStructured(ChatRequest request, Class<T> responseType) {
        // 开启 JSON Mode, 让模型输出合法 JSON
        ChatRequest jsonReq = new ChatRequest(
                request.getMessages(), request.getTemperature(),
                request.getMaxTokens(), true, request.getResponseSchema());
        ChatResponse resp = chat(jsonReq);
        try {
            return objectMapper.readValue(resp.getContent(), responseType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("结构化响应解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean healthCheck() {
        if (apiKey == null || apiKey.isBlank()) return false;
        try {
            ChatRequest ping = ChatRequest.of("Reply with one word: ok");
            ping.setMaxTokens(10);
            chat(ping);
            return true;
        } catch (Exception e) {
            log.warn("[DeepSeek] 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    // ── 内部实现 ──────────────────────────────────────────────

    private ChatResponse executeWithRetry(ChatRequest request) {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return doChat(request);
            } catch (TooManyRequestsException | InsufficientBalanceException e) {
                throw e; // 限流/余额不足不重试, 直接上抛
            } catch (Exception e) {
                last = e;
                if (attempt < MAX_RETRIES) {
                    long backoff = (1L << (attempt - 1)) * retryDelayMs; // 1x / 2x / 4x
                    log.warn("[DeepSeek] 第 {}/{} 次调用失败, {}ms 后重试: {}", attempt, MAX_RETRIES, backoff, e.getMessage());
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new RuntimeException("DeepSeek 调用失败, 已重试 " + MAX_RETRIES + " 次", last);
    }

    private ChatResponse doChat(ChatRequest request) throws IOException {
        String requestBody = buildRequestBody(request);
        Request httpReq = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        long start = System.currentTimeMillis();
        try (Response resp = httpClient.newCall(httpReq).execute()) {
            long latency = System.currentTimeMillis() - start;
            String body = resp.body() != null ? resp.body().string() : "";

            if (resp.code() == 429) {
                throw new TooManyRequestsException("DeepSeek API 限流, 请稍后重试");
            }
            if (resp.code() == 402) {
                throw new InsufficientBalanceException("DeepSeek API 余额不足");
            }
            if (!resp.isSuccessful()) {
                throw new RuntimeException("DeepSeek API 错误 HTTP " + resp.code() + ": " + body);
            }

            return parseResponse(body, latency);
        }
    }

    private String buildRequestBody(ChatRequest req) throws JsonProcessingException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);

        List<Map<String, String>> msgs = new ArrayList<>();
        for (var m : req.getMessages()) {
            msgs.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        body.put("messages", msgs);
        body.put("temperature", req.getTemperature());
        body.put("max_tokens", req.getMaxTokens());

        if (Boolean.TRUE.equals(req.getJsonMode())) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return objectMapper.writeValueAsString(body);
    }

    private ChatResponse parseResponse(String body, long latencyMs) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(body);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        String requestId = root.path("id").asText();
        int promptTokens = root.path("usage").path("prompt_tokens").asInt();
        int completionTokens = root.path("usage").path("completion_tokens").asInt();

        // 不记录 prompt 内容, 只记录 token / latency / 模型名 (隐私保护)
        log.info("[DeepSeek] model={}, prompt_tokens={}, completion_tokens={}, latency={}ms",
                model, promptTokens, completionTokens, latencyMs);

        return ChatResponse.builder()
                .content(content)
                .model(model)
                .requestId(requestId)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .latencyMs(latencyMs)
                .build();
    }

    private static OkHttpClient buildClient(int timeoutSec) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(timeoutSec, TimeUnit.SECONDS)
                .build();
    }
}
