package com.marketai.backend.ai.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketai.backend.ai.insight.dto.InsightResult;
import com.marketai.backend.ai.llm.LLMFactory;
import com.marketai.backend.ai.llm.PromptTemplate;
import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;
import com.marketai.backend.mapper.DashboardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsightAnalyzer 单元测试")
class InsightAnalyzerTest {

    @Mock private LLMFactory     llmFactory;
    @Mock private DashboardMapper dashboardMapper;

    @InjectMocks private InsightAnalyzer analyzer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void injectObjectMapper() {
        // @InjectMocks 不注入非 @Mock 字段, 用反射手动注入
        org.springframework.test.util.ReflectionTestUtils.setField(analyzer, "objectMapper", objectMapper);
    }

    // ── Prompt 模板渲染 ────────────────────────────────────────

    @Nested
    @DisplayName("Prompt 模板渲染")
    class PromptRenderingTest {

        @Test
        @DisplayName("模板文件存在且占位符正确替换")
        void shouldRenderAllPlaceholders() {
            PromptTemplate tpl = PromptTemplate.load("insight_analysis");
            String rendered = tpl.render(java.util.Map.of(
                    "productDescription", "一款 B2B SaaS 客服系统",
                    "industry", "企业服务",
                    "targetMarket", "中国",
                    "keywords", "AI客服、智能客服",
                    "trendDataSection", "",
                    "depthHint", ""
            ));

            assertThat(rendered).contains("B2B SaaS 客服系统");
            assertThat(rendered).contains("企业服务");
            assertThat(rendered).contains("AI客服、智能客服");
            assertThat(rendered).doesNotContain("{{productDescription}}");
            assertThat(rendered).doesNotContain("{{keywords}}");
        }

        @Test
        @DisplayName("有趋势数据时 trendDataSection 注入到 prompt")
        void shouldInjectTrendSummaryWhenPresent() {
            PromptTemplate tpl = PromptTemplate.load("insight_analysis");
            String trend = "过去30天搜索热度均值 1234，峰值 3000，整体趋势上升";
            String rendered = tpl.render(java.util.Map.of(
                    "productDescription", "产品描述",
                    "industry", "行业",
                    "targetMarket", "市场",
                    "keywords", "关键词",
                    "trendDataSection", "\n近期趋势数据摘要:\n" + trend + "\n",
                    "depthHint", ""
            ));

            assertThat(rendered).contains("近期趋势数据摘要");
            assertThat(rendered).contains(trend);
        }
    }

    // ── JSON 验证 ──────────────────────────────────────────────

    @Nested
    @DisplayName("AI JSON 输出验证")
    class JsonValidationTest {

        @Test
        @DisplayName("合法 JSON — 返回 InsightResult 并通过校验")
        void shouldParseValidJson() {
            String validJson = buildValidJson();
            mockLLMResponse(validJson, 3000);

            InsightAnalyzer.AnalysisOutput output = analyzer.analyze(
                    1L, "产品描述", "行业", "目标市场", List.of("关键词"), "standard");

            assertThat(output.result().getMarketFitScore()).isEqualTo(72);
            assertThat(output.result().getPainPoints()).hasSize(3);
            assertThat(output.result().getOpportunities()).hasSize(3);
            assertThat(output.result().getRisks()).hasSize(2);
            assertThat(output.result().getActions()).hasSize(3);
            assertThat(output.tokensUsed()).isEqualTo(3000);
        }

        @Test
        @DisplayName("痛点少于 3 个 — 触发 JSON 重试，第二次成功")
        void shouldRetryOnInsufficientPainPoints() {
            String badJson  = buildJsonWithPainPointCount(2);
            String goodJson = buildValidJson();

            var mockProvider = mock(com.marketai.backend.ai.llm.LLMProvider.class);
            when(llmFactory.getProvider()).thenReturn(mockProvider);
            when(mockProvider.chat(any()))
                    .thenReturn(ChatResponse.builder().content(badJson).totalTokens(1000).model("deepseek-chat").build())
                    .thenReturn(ChatResponse.builder().content(goodJson).totalTokens(3000).model("deepseek-chat").build());

            InsightAnalyzer.AnalysisOutput output = analyzer.analyze(
                    1L, "产品描述", "行业", "目标市场", List.of("关键词"), "standard");

            verify(mockProvider, times(2)).chat(any(ChatRequest.class));
            assertThat(output.result().getPainPoints()).hasSize(3);
        }

        @Test
        @DisplayName("非法 JSON — 重试 1 次后仍失败，抛出 RuntimeException")
        void shouldFailAfterJsonRetryExhausted() {
            String invalidJson = "这不是 JSON";
            var mockProvider = mock(com.marketai.backend.ai.llm.LLMProvider.class);
            when(llmFactory.getProvider()).thenReturn(mockProvider);
            when(mockProvider.chat(any()))
                    .thenReturn(ChatResponse.builder().content(invalidJson).totalTokens(500).model("deepseek-chat").build())
                    .thenReturn(ChatResponse.builder().content(invalidJson).totalTokens(500).model("deepseek-chat").build());

            assertThatThrownBy(() ->
                    analyzer.analyze(1L, "产品描述", "行业", "目标市场", List.of("关键词"), "standard"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("JSON 验证失败");

            verify(mockProvider, times(2)).chat(any());
        }

        @Test
        @DisplayName("网络错误 — 重试 2 次后成功")
        void shouldRetryOnNetworkErrorAndSucceed() {
            String goodJson = buildValidJson();
            var mockProvider = mock(com.marketai.backend.ai.llm.LLMProvider.class);
            when(llmFactory.getProvider()).thenReturn(mockProvider);
            when(mockProvider.chat(any()))
                    .thenThrow(new RuntimeException("Connection refused"))
                    .thenThrow(new RuntimeException("Connection refused"))
                    .thenReturn(ChatResponse.builder().content(goodJson).totalTokens(3000).model("deepseek-chat").build());

            InsightAnalyzer.AnalysisOutput output = analyzer.analyze(
                    1L, "产品描述", "行业", "目标市场", List.of("关键词"), "standard");

            verify(mockProvider, times(3)).chat(any());
            assertThat(output.result()).isNotNull();
        }

        @Test
        @DisplayName("网络错误超过 3 次 — 抛出 RuntimeException")
        void shouldFailAfterNetworkRetryExhausted() {
            var mockProvider = mock(com.marketai.backend.ai.llm.LLMProvider.class);
            when(llmFactory.getProvider()).thenReturn(mockProvider);
            when(mockProvider.chat(any())).thenThrow(new RuntimeException("Timeout"));

            assertThatThrownBy(() ->
                    analyzer.analyze(1L, "产品描述", "行业", "目标市场", List.of("关键词"), "standard"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("网络错误");

            verify(mockProvider, times(3)).chat(any());
        }
    }

    // ── 工具方法 ──────────────────────────────────────────────

    private void mockLLMResponse(String content, int tokens) {
        var mockProvider = mock(com.marketai.backend.ai.llm.LLMProvider.class);
        when(llmFactory.getProvider()).thenReturn(mockProvider);
        when(mockProvider.chat(any())).thenReturn(
                ChatResponse.builder().content(content).totalTokens(tokens).model("deepseek-chat").build());
    }

    private String buildValidJson() {
        return """
                {
                  "marketFitScore": 72,
                  "dimensions": {
                    "demandStrength": 80,
                    "competitionSaturation": 60,
                    "growthPotential": 75,
                    "entryBarrier": 55
                  },
                  "painPoints": [
                    {"title": "痛点1", "severity": "high", "description": "描述1", "evidence": ["证据A"]},
                    {"title": "痛点2", "severity": "medium", "description": "描述2", "evidence": ["证据B"]},
                    {"title": "痛点3", "severity": "low", "description": "描述3", "evidence": ["证据C"]}
                  ],
                  "opportunities": [
                    {"name": "机会1", "type": "blue_ocean", "description": "描述", "tags": ["标签1"]},
                    {"name": "机会2", "type": "high_value", "description": "描述", "tags": ["标签2"]},
                    {"name": "机会3", "type": "differentiation", "description": "描述", "tags": ["标签3"]}
                  ],
                  "risks": [
                    {"name": "风险1", "level": "high", "description": "描述"},
                    {"name": "风险2", "level": "medium", "description": "描述"}
                  ],
                  "actions": [
                    {"title": "建议1", "description": "描述"},
                    {"title": "建议2", "description": "描述"},
                    {"title": "建议3", "description": "描述"}
                  ]
                }
                """;
    }

    private String buildJsonWithPainPointCount(int count) {
        StringBuilder pp = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) pp.append(",");
            pp.append("{\"title\":\"痛点").append(i).append("\",\"severity\":\"high\",\"description\":\"描述\",\"evidence\":[]}");
        }
        pp.append("]");
        return """
                {
                  "marketFitScore": 60,
                  "dimensions": {"demandStrength": 70, "competitionSaturation": 50, "growthPotential": 65, "entryBarrier": 45},
                  "painPoints": %s,
                  "opportunities": [
                    {"name": "机会1", "type": "blue_ocean", "description": "描述", "tags": []},
                    {"name": "机会2", "type": "high_value", "description": "描述", "tags": []},
                    {"name": "机会3", "type": "differentiation", "description": "描述", "tags": []}
                  ],
                  "risks": [
                    {"name": "风险1", "level": "high", "description": "描述"},
                    {"name": "风险2", "level": "medium", "description": "描述"}
                  ],
                  "actions": [
                    {"title": "建议1", "description": "描述"},
                    {"title": "建议2", "description": "描述"},
                    {"title": "建议3", "description": "描述"}
                  ]
                }
                """.formatted(pp);
    }
}
