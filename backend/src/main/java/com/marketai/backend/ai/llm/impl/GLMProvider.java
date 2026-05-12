package com.marketai.backend.ai.llm.impl;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSignerUtil;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 智谱 GLM 提供商
 * API 兼容 OpenAI 格式, 但认证使用 JWT (非直接 Bearer Key)
 *
 * 认证方式: API Key 格式为 "{user_id}.{secret}",
 *   需用 HS256 生成 JWT 作为 Bearer Token, exp/timestamp 单位是毫秒 (非标准秒)
 */
@Slf4j
@Component
public class GLMProvider implements LLMProvider {

    private static final int MAX_RETRIES = 3;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final long retryDelayMs;

    @Autowired
    public GLMProvider(
            @Value("${marketai.ai.glm.api-key:}") String apiKey,
            @Value("${marketai.ai.glm.model:glm-4-plus}") String model,
            ObjectMapper objectMapper) {
        this(apiKey, model, buildClient(30), objectMapper, 1000L);
    }

    GLMProvider(String apiKey, String model, OkHttpClient client, ObjectMapper objectMapper, long retryDelayMs) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = client;
        this.objectMapper = objectMapper;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public String getName() {
        return "glm";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return executeWithRetry(request);
    }

    @Override
    public <T> T chatStructured(ChatRequest request, Class<T> responseType) {
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
        if (apiKey == null || apiKey.isBlank() || !apiKey.contains(".")) return false;
        try {
            ChatRequest ping = ChatRequest.of("Reply with one word: ok");
            ping.setMaxTokens(10);
            chat(ping);
            return true;
        } catch (Exception e) {
            log.warn("[GLM] 健康检查失败: {}", e.getMessage());
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
                throw e;
            } catch (Exception e) {
                last = e;
                if (attempt < MAX_RETRIES) {
                    long backoff = (1L << (attempt - 1)) * retryDelayMs;
                    log.warn("[GLM] 第 {}/{} 次调用失败, {}ms 后重试: {}", attempt, MAX_RETRIES, backoff, e.getMessage());
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new RuntimeException("GLM 调用失败, 已重试 " + MAX_RETRIES + " 次", last);
    }

    private ChatResponse doChat(ChatRequest request) throws IOException {
        String token = generateJwtToken();
        String requestBody = buildRequestBody(request);

        Request httpReq = new Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        long start = System.currentTimeMillis();
        try (Response resp = httpClient.newCall(httpReq).execute()) {
            long latency = System.currentTimeMillis() - start;
            String body = resp.body() != null ? resp.body().string() : "";

            if (resp.code() == 429) throw new TooManyRequestsException("GLM API 限流, 请稍后重试");
            if (resp.code() == 402) throw new InsufficientBalanceException("GLM API 余额不足");
            if (!resp.isSuccessful()) throw new RuntimeException("GLM API 错误 HTTP " + resp.code() + ": " + body);

            return parseResponse(body, latency);
        }
    }

    /**
     * GLM JWT 生成: API Key 格式 "{user_id}.{secret}"
     * exp / timestamp 使用毫秒时间戳 (GLM 非标准设计)
     */
    private String generateJwtToken() {
        String[] parts = apiKey.split("\\.", 2);
        if (parts.length != 2) throw new IllegalStateException("GLM API Key 格式错误, 期望 '{user_id}.{secret}'");
        String keyId = parts[0];
        String secret = parts[1];
        long now = System.currentTimeMillis();

        return JWT.create()
                .setHeader("alg", "HS256")
                .setHeader("sign_type", "SIGN")
                .setPayload("api_key", keyId)
                .setPayload("exp", now + 3_600_000L) // 1小时, 单位毫秒
                .setPayload("timestamp", now)
                .sign(JWTSignerUtil.hs256(secret.getBytes(StandardCharsets.UTF_8)));
    }

    private String buildRequestBody(ChatRequest req) throws JsonProcessingException {
        List<Map<String, String>> msgs = new ArrayList<>();
        for (var m : req.getMessages()) {
            msgs.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
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

        log.info("[GLM] model={}, prompt_tokens={}, completion_tokens={}, latency={}ms",
                model, promptTokens, completionTokens, latencyMs);

        return ChatResponse.builder()
                .content(content).model(model).requestId(requestId)
                .promptTokens(promptTokens).completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens).latencyMs(latencyMs)
                .build();
    }

    private static OkHttpClient buildClient(int timeoutSec) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .build();
    }
}
