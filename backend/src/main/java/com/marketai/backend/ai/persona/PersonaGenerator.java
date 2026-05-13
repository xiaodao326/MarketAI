package com.marketai.backend.ai.persona;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.llm.LLMFactory;
import com.marketai.backend.ai.llm.PromptTemplate;
import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;
import com.marketai.backend.ai.llm.exception.InsufficientBalanceException;
import com.marketai.backend.ai.llm.exception.TooManyRequestsException;
import com.marketai.backend.ai.persona.dto.PersonaResult;
import com.marketai.backend.entity.InsightReport;
import com.marketai.backend.mapper.InsightReportMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 用户画像生成器
 *
 * 重试策略与 InsightAnalyzer 一致:
 *   - 网络/服务异常: 最多重试 2 次 (共 3 次调用)
 *   - JSON 验证失败: 最多重试 1 次 (共 2 次调用)
 *
 * 上下文增强: 如果项目已有 completed 状态的洞察报告, 会提取 top 痛点喂给 prompt,
 * 让 AI 生成的画像与已知痛点对齐, 提升一致性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaGenerator {

    private static final int NETWORK_MAX_ATTEMPTS = 3;
    private static final int JSON_MAX_ATTEMPTS    = 2;

    private final LLMFactory          llmFactory;
    private final InsightReportMapper insightReportMapper;
    private final ObjectMapper        objectMapper;

    public record GenerationOutput(List<PersonaResult> personas, int tokensUsed, String modelName) {}

    /**
     * 生成画像
     *
     * @param projectId          项目 ID (用于查最新洞察报告作上下文)
     * @param productDescription 产品描述
     * @param industry           行业
     * @param targetMarket       目标市场
     * @param keywords           关键词
     * @param count              生成数量 3-5
     * @param extraContext       用户额外补充的上下文 (可空)
     */
    public GenerationOutput generate(Long projectId, String productDescription,
                                     String industry, String targetMarket,
                                     List<String> keywords, int count, String extraContext) {

        String insightSection = buildInsightContextSection(projectId);
        String extraSection   = buildExtraContextSection(extraContext);
        String prompt         = buildPrompt(productDescription, industry, targetMarket,
                                            keywords, count, insightSection, extraSection);
        ChatRequest req       = buildRequest(prompt);

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
                    throw new RuntimeException("[PersonaGenerator] 网络错误，已重试 "
                            + (networkAttempts - 1) + " 次: " + e.getMessage(), e);
                }
                log.warn("[PersonaGenerator] 网络错误，第 {}/{} 次重试: {}",
                        networkAttempts, NETWORK_MAX_ATTEMPTS - 1, e.getMessage());
                continue;
            }

            try {
                List<PersonaResult> personas = validateAndParse(resp.getContent(), count);
                log.info("[PersonaGenerator] 画像生成完成: model={}, tokens={}, count={}",
                        resp.getModel(), resp.getTotalTokens(), personas.size());
                return new GenerationOutput(personas, resp.getTotalTokens(), resp.getModel());
            } catch (JsonValidationException e) {
                jsonAttempts++;
                if (jsonAttempts >= JSON_MAX_ATTEMPTS) {
                    throw new RuntimeException("[PersonaGenerator] JSON 验证失败，已重试 "
                            + (jsonAttempts - 1) + " 次: " + e.getMessage(), e);
                }
                log.warn("[PersonaGenerator] JSON 验证失败，第 {}/{} 次重新请求: {}",
                        jsonAttempts, JSON_MAX_ATTEMPTS - 1, e.getMessage());
                networkAttempts = 0;
            }
        }
    }

    // ── Prompt 构建 ────────────────────────────────────────────

    private ChatRequest buildRequest(String prompt) {
        ChatRequest req = ChatRequest.of(prompt);
        req.setMaxTokens(4000);
        req.setTemperature(0.7); // 画像生成需要多样性, 温度比 insight 高
        req.setJsonMode(true);
        return req;
    }

    private String buildPrompt(String productDescription, String industry, String targetMarket,
                               List<String> keywords, int count,
                               String insightSection, String extraSection) {
        return PromptTemplate.load("persona_generation").render(Map.of(
                "productDescription",     productDescription,
                "industry",               industry,
                "targetMarket",           targetMarket,
                "keywords",               String.join("、", keywords),
                "count",                  String.valueOf(count),
                "insightContextSection",  insightSection,
                "extraContextSection",    extraSection
        ));
    }

    /** 提取最新洞察报告的 top 3 痛点作为画像生成的上下文 */
    private String buildInsightContextSection(Long projectId) {
        try {
            InsightReport latest = insightReportMapper.selectOne(
                    new LambdaQueryWrapper<InsightReport>()
                            .eq(InsightReport::getProjectId, projectId)
                            .eq(InsightReport::getStatus, "completed")
                            .orderByDesc(InsightReport::getCreatedAt)
                            .last("LIMIT 1"));
            if (latest == null || latest.getPainPoints() == null) return "";

            JsonNode arr = objectMapper.readTree(latest.getPainPoints());
            if (!arr.isArray() || arr.isEmpty()) return "";

            String top = StreamSupport.stream(arr.spliterator(), false)
                    .limit(3)
                    .map(node -> "- " + node.path("title").asText() + ": " + node.path("description").asText(""))
                    .collect(Collectors.joining("\n"));
            return "\n已知核心痛点(来自最近的洞察分析):\n" + top + "\n";
        } catch (Exception e) {
            log.warn("[PersonaGenerator] 提取洞察上下文失败: {}", e.getMessage());
            return "";
        }
    }

    private String buildExtraContextSection(String extra) {
        if (extra == null || extra.isBlank()) return "";
        return "\n补充上下文: " + extra.trim() + "\n";
    }

    // ── JSON 验证 ──────────────────────────────────────────────

    private List<PersonaResult> validateAndParse(String rawJson, int expectedCount) throws JsonValidationException {
        List<PersonaResult> personas;
        try {
            // 模型可能返回 {"personas": [...]} 或 [...]; 容忍两种
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode arr  = root.isArray() ? root : root.has("personas") ? root.get("personas") : root.elements().hasNext() ? root.elements().next() : null;
            if (arr == null || !arr.isArray()) {
                throw new JsonValidationException("响应不是 JSON 数组");
            }
            personas = objectMapper.convertValue(arr, new TypeReference<>() {});
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new JsonValidationException("JSON 解析失败: " + e.getMessage());
        }

        if (personas == null || personas.isEmpty()) {
            throw new JsonValidationException("画像数组为空");
        }
        // 容忍 ±1 偏差 — 模型偶尔返回 n±1, 不强制阻断
        if (Math.abs(personas.size() - expectedCount) > 1) {
            throw new JsonValidationException("画像数量与请求不符: 期望 " + expectedCount + ", 实际 " + personas.size());
        }

        for (int i = 0; i < personas.size(); i++) {
            PersonaResult p = personas.get(i);
            if (p.getName() == null || p.getName().isBlank())
                throw new JsonValidationException("第 " + (i + 1) + " 个画像缺少 name");
            if (p.getRole() == null || p.getRole().isBlank())
                throw new JsonValidationException("第 " + (i + 1) + " 个画像缺少 role");
            if (p.getDecisionParams() == null)
                throw new JsonValidationException("第 " + (i + 1) + " 个画像缺少 decisionParams");
        }
        return personas;
    }

    // ── 内部异常类 ─────────────────────────────────────────────

    static class JsonValidationException extends Exception {
        JsonValidationException(String message) { super(message); }
    }
}
