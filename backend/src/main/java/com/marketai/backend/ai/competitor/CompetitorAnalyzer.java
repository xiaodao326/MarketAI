package com.marketai.backend.ai.competitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.competitor.dto.CompetitorAnalysisResult;
import com.marketai.backend.ai.llm.LLMFactory;
import com.marketai.backend.ai.llm.PromptTemplate;
import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;
import com.marketai.backend.ai.llm.exception.InsufficientBalanceException;
import com.marketai.backend.ai.llm.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 竞品分析器
 *
 * 重试策略与 InsightAnalyzer / PersonaGenerator 一致:
 *   - 网络/服务异常: 最多重试 2 次 (共 3 次调用)
 *   - JSON 验证失败: 最多重试 1 次 (共 2 次调用)
 *   - TooManyRequestsException / InsufficientBalanceException: 不重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitorAnalyzer {

    private static final int NETWORK_MAX_ATTEMPTS = 3;
    private static final int JSON_MAX_ATTEMPTS    = 2;

    private final LLMFactory   llmFactory;
    private final ObjectMapper objectMapper;

    public record AnalysisOutput(CompetitorAnalysisResult result, int tokensUsed, String modelName) {}

    public AnalysisOutput analyze(String productDescription, String industry,
                                  String targetMarket, List<String> competitorNames) {

        String prompt   = buildPrompt(productDescription, industry, targetMarket, competitorNames);
        ChatRequest req = buildRequest(prompt);

        int networkAttempts = 0;
        int jsonAttempts    = 0;

        while (true) {
            ChatResponse resp;
            try {
                resp = llmFactory.getProvider().chat(req);
            } catch (TooManyRequestsException | InsufficientBalanceException e) {
                throw e;
            } catch (RuntimeException e) {
                networkAttempts++;
                if (networkAttempts >= NETWORK_MAX_ATTEMPTS) {
                    throw new RuntimeException("[CompetitorAnalyzer] 网络错误,已重试 "
                            + (networkAttempts - 1) + " 次: " + e.getMessage(), e);
                }
                log.warn("[CompetitorAnalyzer] 网络错误,第 {}/{} 次重试: {}",
                        networkAttempts, NETWORK_MAX_ATTEMPTS - 1, e.getMessage());
                continue;
            }

            try {
                CompetitorAnalysisResult result = validateAndParse(resp.getContent(), competitorNames);
                log.info("[CompetitorAnalyzer] 分析完成: model={}, tokens={}, competitors={}",
                        resp.getModel(), resp.getTotalTokens(),
                        result.getCompetitors() != null ? result.getCompetitors().size() : 0);
                return new AnalysisOutput(result, resp.getTotalTokens(), resp.getModel());
            } catch (JsonValidationException e) {
                jsonAttempts++;
                if (jsonAttempts >= JSON_MAX_ATTEMPTS) {
                    throw new RuntimeException("[CompetitorAnalyzer] JSON 验证失败,已重试 "
                            + (jsonAttempts - 1) + " 次: " + e.getMessage(), e);
                }
                log.warn("[CompetitorAnalyzer] JSON 验证失败,第 {}/{} 次重新请求: {}",
                        jsonAttempts, JSON_MAX_ATTEMPTS - 1, e.getMessage());
                networkAttempts = 0;
            }
        }
    }

    // ── Prompt 构建 ────────────────────────────────────────────

    private ChatRequest buildRequest(String prompt) {
        ChatRequest req = ChatRequest.of(prompt);
        req.setMaxTokens(5000);
        req.setTemperature(0.4);
        req.setJsonMode(true);
        return req;
    }

    private String buildPrompt(String productDescription, String industry,
                               String targetMarket, List<String> competitorNames) {
        return PromptTemplate.load("competitor_analysis").render(Map.of(
                "productDescription", productDescription,
                "industry",           industry,
                "targetMarket",       targetMarket,
                "competitorNames",    String.join("、", competitorNames)
        ));
    }

    // ── JSON 验证 ──────────────────────────────────────────────

    private CompetitorAnalysisResult validateAndParse(String rawJson, List<String> requested) throws JsonValidationException {
        CompetitorAnalysisResult result;
        try {
            result = objectMapper.readValue(rawJson, CompetitorAnalysisResult.class);
        } catch (JsonProcessingException e) {
            throw new JsonValidationException("JSON 解析失败: " + e.getMessage());
        }

        if (result.getCompetitors() == null || result.getCompetitors().isEmpty()) {
            throw new JsonValidationException("competitors 数组为空");
        }
        // 容忍 ±1 — AI 偶尔合并或拆分名单
        if (Math.abs(result.getCompetitors().size() - requested.size()) > 1) {
            throw new JsonValidationException("竞品数量与请求不符: 期望 " + requested.size()
                    + ", 实际 " + result.getCompetitors().size());
        }

        if (result.getFeatureMatrix() == null
                || result.getFeatureMatrix().getFeatures() == null
                || result.getFeatureMatrix().getFeatures().isEmpty()) {
            throw new JsonValidationException("featureMatrix.features 缺失");
        }
        if (result.getFeatureMatrix().getFeatures().size() < 4) {
            throw new JsonValidationException("featureMatrix.features 太少: 至少 4 个");
        }

        // 校验每个竞品的 scores 数组长度对齐 features
        int featureCount = result.getFeatureMatrix().getFeatures().size();
        if (result.getFeatureMatrix().getScores() != null) {
            for (Map.Entry<String, List<String>> e : result.getFeatureMatrix().getScores().entrySet()) {
                if (e.getValue() == null || e.getValue().size() != featureCount) {
                    throw new JsonValidationException("scores['" + e.getKey()
                            + "'] 长度不等于 features 数量 " + featureCount);
                }
            }
        }

        if (result.getDifferentiationInsights() == null || result.getDifferentiationInsights().size() < 3) {
            throw new JsonValidationException("differentiationInsights 至少 3 条");
        }

        return result;
    }

    // ── 内部异常类 ─────────────────────────────────────────────

    static class JsonValidationException extends Exception {
        JsonValidationException(String message) { super(message); }
    }
}
