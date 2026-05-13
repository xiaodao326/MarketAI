package com.marketai.backend.ai.competitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI 返回的竞品分析结果 — 与 prompt JSON schema 严格对应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetitorAnalysisResult {

    private List<CompetitorItem> competitors;
    private FeatureMatrix featureMatrix;
    private List<DifferentiationInsight> differentiationInsights;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompetitorItem {
        private String name;
        /** direct / indirect / potential */
        private String type;
        private String region;
        private BigDecimal rating;
        private String pricingModel;
        private String fundingStage;
        /** high / medium / low */
        private String threatLevel;
        private List<String> strengths;
        private List<String> weaknesses;
        private Boolean needsUserInput;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeatureMatrix {
        /** 跨竞品的核心功能列表 (6-8 个) */
        private List<String> features;
        /** 每个竞品的支持度数组, 顺序对应 features: ["yes"|"partial"|"no"] */
        private Map<String, List<String>> scores;
        /** 机会标记, 如: "功能1: 空白" / "功能2: 红海" */
        private List<String> opportunities;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DifferentiationInsight {
        /** core_opportunity / pricing_strategy / warning */
        private String type;
        private String title;
        private String description;
    }
}
