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
 * 通义千问提供商 (阿里云 DashScope)
 * API 格式与 OpenAI 不同: input.messages + parameters 结构
 * Endpoint: POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
 */
@Slf4j
@Component
public class QwenProvider implements LLMProvider {

    private static final int MAX_RETRIES = 3;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ENDPOINT = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final long retryDelayMs;

    @Autowired
    public QwenProvider(
            @Value("${marketai.ai.qwen.api-key:}") String apiKey,
            @Value("${marketai.ai.qwen.model:qwen-plus}") String model,
            ObjectMapper objectMapper) {
        this(apiKey, model, buildClient(30), objectMapper, 1000L);
    }

    QwenProvider(String apiKey, String model, OkHttpClient client, ObjectMapper objectMapper, long retryDelayMs) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = client;
        this.objectMapper = objectMapper;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public String getName() {
        return "qwen";
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
        if (apiKey == null || apiKey.isBlank()) return false;
        try {
            ChatRequest ping = ChatRequest.of("Reply with one word: ok");
            ping.setMaxTokens(10);
            chat(ping);
            return true;
        } catch (Exception e) {
            log.warn("[Qwen] 健康检查失败: {}", e.getMessage());
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
                    log.warn("[Qwen] 第 {}/{} 次调用失败, {}ms 后重试: {}", attempt, MAX_RETRIES, backoff, e.getMessage());
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new RuntimeException("Qwen 调用失败, 已重试 " + MAX_RETRIES + " 次", last);
    }

    private ChatResponse doChat(ChatRequest request) throws IOException {
        String requestBody = buildRequestBody(request);
        Request httpReq = new Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        long start = System.currentTimeMillis();
        try (Response resp = httpClient.newCall(httpReq).execute()) {
            long latency = System.currentTimeMillis() - start;
            String body = resp.body() != null ? resp.body().string() : "";

            if (resp.code() == 429) throw new TooManyRequestsException("Qwen API 限流, 请稍后重试");
            if (resp.code() == 402) throw new InsufficientBalanceException("Qwen API 余额不足");
            if (!resp.isSuccessful()) throw new RuntimeException("Qwen API 错误 HTTP " + resp.code() + ": " + body);

            // DashScope 也在 body 中返回错误码
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("code").asText();
            if (!code.isBlank() && !"null".equals(code)) {
                if (code.startsWith("Throttling")) throw new TooManyRequestsException("Qwen 限流: " + code);
                throw new RuntimeException("Qwen API 业务错误: " + code + " " + root.path("message").asText());
            }

            return parseResponse(root, latency);
        }
    }

    /**
     * DashScope 请求体格式: {model, input:{messages}, parameters:{...}}
     */
    private String buildRequestBody(ChatRequest req) throws JsonProcessingException {
        List<Map<String, String>> msgs = new ArrayList<>();
        for (var m : req.getMessages()) {
            msgs.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("result_format", "message");
        parameters.put("temperature", req.getTemperature());
        parameters.put("max_tokens", req.getMaxTokens());
        if (Boolean.TRUE.equals(req.getJsonMode())) {
            parameters.put("response_format", Map.of("type", "json_object"));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", Map.of("messages", msgs));
        body.put("parameters", parameters);
        return objectMapper.writeValueAsString(body);
    }

    /**
     * DashScope 响应结构: output.choices[0].message.content + usage.input_tokens
     */
    private ChatResponse parseResponse(JsonNode root, long latencyMs) {
        String content = root.path("output").path("choices").path(0)
                .path("message").path("content").asText();
        String requestId = root.path("request_id").asText();
        int promptTokens = root.path("usage").path("input_tokens").asInt();
        int completionTokens = root.path("usage").path("output_tokens").asInt();

        log.info("[Qwen] model={}, prompt_tokens={}, completion_tokens={}, latency={}ms",
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
