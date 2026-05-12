package com.marketai.backend.ai.insight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.insight.dto.InsightResult;
import com.marketai.backend.ai.llm.LLMFactory;
import com.marketai.backend.ai.llm.PromptTemplate;
import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;
import com.marketai.backend.ai.llm.exception.InsufficientBalanceException;
import com.marketai.backend.ai.llm.exception.TooManyRequestsException;
import com.marketai.backend.dto.dashboard.DailyMetricDTO;
import com.marketai.backend.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 需求洞察核心分析器
 *
 * 重试策略:
 *   - 网络/服务异常: 最多重试 2 次 (共 3 次调用)
 *   - JSON 验证失败: 最多重试 1 次 (共 2 次调用)
 *   - TooManyRequestsException / InsufficientBalanceException: 不重试, 直接上抛
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightAnalyzer {

    private static final int NETWORK_MAX_ATTEMPTS = 3; // 2 重试
    private static final int JSON_MAX_ATTEMPTS    = 2; // 1 重试
    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    private final LLMFactory    llmFactory;
    private final DashboardMapper dashboardMapper;
    private final ObjectMapper  objectMapper;

    public record AnalysisOutput(InsightResult result, int tokensUsed, String modelName) {}

    /**
     * 执行洞察分析，包含完整的重试逻辑。
     *
     * @param projectId          项目 ID (用于获取趋势数据摘要)
     * @param productDescription 产品描述
     * @param industry           所属行业
     * @param targetMarket       目标市场
     * @param keywords           监控关键词
     * @param depth              分析深度: lite / standard / full
     */
    public AnalysisOutput analyze(Long projectId, String productDescription,
                                  String industry, String targetMarket,
                                  List<String> keywords, String depth) {

        String trendSummary = buildTrendSummary(projectId);
        String prompt       = buildPrompt(productDescription, industry, targetMarket, keywords, trendSummary, depth);
        ChatRequest req     = buildRequest(prompt, depth);

        int networkAttempts = 0;
        int jsonAttempts    = 0;

        while (true) {
            ChatResponse resp;
            try {
                resp = llmFactory.getProvider().chat(req);
            } catch (TooManyRequestsException | InsufficientBalanceException e) {
                throw e; // 不重试
            } catch (RuntimeException e) {
                networkAttempts++;
                if (networkAttempts >= NETWORK_MAX_ATTEMPTS) {
                    throw new RuntimeException("[InsightAnalyzer] 网络错误，已重试 " + (networkAttempts - 1) + " 次: " + e.getMessage(), e);
                }
                log.warn("[InsightAnalyzer] 网络错误，第 {}/{} 次重试: {}", networkAttempts, NETWORK_MAX_ATTEMPTS - 1, e.getMessage());
                continue;
            }

            try {
                InsightResult result = validateAndParse(resp.getContent());
                log.info("[InsightAnalyzer] 分析完成: model={}, tokens={}, marketFitScore={}",
                        resp.getModel(), resp.getTotalTokens(), result.getMarketFitScore());
                return new AnalysisOutput(result, resp.getTotalTokens(), resp.getModel());
            } catch (JsonValidationException e) {
                jsonAttempts++;
                if (jsonAttempts >= JSON_MAX_ATTEMPTS) {
                    throw new RuntimeException("[InsightAnalyzer] JSON 验证失败，已重试 " + (jsonAttempts - 1) + " 次: " + e.getMessage(), e);
                }
                log.warn("[InsightAnalyzer] JSON 验证失败，第 {}/{} 次重新请求: {}", jsonAttempts, JSON_MAX_ATTEMPTS - 1, e.getMessage());
                // 重新请求 AI (网络重试计数重置)
                networkAttempts = 0;
            }
        }
    }

    // ── Prompt 构建 ────────────────────────────────────────────

    private ChatRequest buildRequest(String prompt, String depth) {
        int maxTokens = switch (depth) {
            case "lite" -> 2000;
            case "full" -> 6000;
            default     -> 4000;
        };
        ChatRequest req = ChatRequest.of(prompt);
        req.setMaxTokens(maxTokens);
        req.setTemperature(0.5);
        req.setJsonMode(true);
        return req;
    }

    private String buildPrompt(String productDescription, String industry,
                               String targetMarket, List<String> keywords,
                               String trendSummary, String depth) {

        String trendDataSection = trendSummary != null
                ? "\n近期趋势数据摘要:\n" + trendSummary + "\n"
                : "";

        String depthHint = switch (depth) {
            case "lite" -> "\n注意：请生成简洁版报告，每个维度约100字以内。";
            case "full" -> "\n注意：请生成详尽版报告，每个维度需充分的数据支撑，约300字。";
            default     -> "";
        };

        return PromptTemplate.load("insight_analysis").render(Map.of(
                "productDescription", productDescription,
                "industry",           industry,
                "targetMarket",       targetMarket,
                "keywords",           String.join("、", keywords),
                "trendDataSection",   trendDataSection,
                "depthHint",          depthHint
        ));
    }

    // ── JSON 验证 ──────────────────────────────────────────────

    private InsightResult validateAndParse(String rawJson) throws JsonValidationException {
        InsightResult result;
        try {
            result = objectMapper.readValue(rawJson, InsightResult.class);
        } catch (JsonProcessingException e) {
            throw new JsonValidationException("JSON 解析失败: " + e.getMessage());
        }

        if (result.getDimensions() == null) {
            throw new JsonValidationException("缺少 dimensions 字段");
        }
        int pp = result.getPainPoints() == null ? 0 : result.getPainPoints().size();
        if (pp < 3) throw new JsonValidationException("痛点数量不足 3 个，实际: " + pp);

        int op = result.getOpportunities() == null ? 0 : result.getOpportunities().size();
        if (op < 3) throw new JsonValidationException("机会数量不足 3 个，实际: " + op);

        int rk = result.getRisks() == null ? 0 : result.getRisks().size();
        if (rk < 2) throw new JsonValidationException("风险数量不足 2 个，实际: " + rk);

        int ac = result.getActions() == null ? 0 : result.getActions().size();
        if (ac < 3) throw new JsonValidationException("行动建议数量不足 3 个，实际: " + ac);

        return result;
    }

    // ── 趋势数据摘要 ───────────────────────────────────────────

    private String buildTrendSummary(Long projectId) {
        try {
            LocalDate today     = LocalDate.now(SH);
            LocalDate startDate = today.minusDays(29);
            List<DailyMetricDTO> data = dashboardMapper.selectDailyTotals(projectId, startDate, today);

            List<Long> searchVals = data.stream()
                    .filter(d -> "baidu_index".equals(d.getSource()) && "search_volume".equals(d.getMetricType()))
                    .map(d -> d.getDailyTotal().longValue())
                    .collect(Collectors.toList());

            if (searchVals.isEmpty()) return null;

            LongSummaryStatistics stats = searchVals.stream().mapToLong(Long::longValue).summaryStatistics();
            int mid    = searchVals.size() / 2;
            long first  = searchVals.subList(0, mid).stream().mapToLong(Long::longValue).sum();
            long second = searchVals.subList(mid, searchVals.size()).stream().mapToLong(Long::longValue).sum();
            String trend = second > first * 1.1 ? "上升" : second < first * 0.9 ? "下降" : "平稳";

            return String.format("过去30天搜索热度均值 %d，峰值 %d，整体趋势%s",
                    Math.round(stats.getAverage()), stats.getMax(), trend);
        } catch (Exception e) {
            log.warn("[InsightAnalyzer] 获取趋势数据摘要失败: {}", e.getMessage());
            return null;
        }
    }

    // ── 内部异常类 ─────────────────────────────────────────────

    static class JsonValidationException extends Exception {
        JsonValidationException(String message) { super(message); }
    }
}
