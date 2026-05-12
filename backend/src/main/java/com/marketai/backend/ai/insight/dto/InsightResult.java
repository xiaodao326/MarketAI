package com.marketai.backend.ai.insight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI 返回的结构化洞察报告 — 与 prompt 中 JSON schema 严格对应。
 * 使用 @JsonIgnoreProperties 避免模型偶尔多输出字段时报错。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsightResult {

    private int marketFitScore;
    private Dimensions dimensions;
    private List<PainPoint> painPoints;
    private List<Opportunity> opportunities;
    private List<Risk> risks;
    private List<ActionItem> actions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dimensions {
        private int demandStrength;
        private int competitionSaturation;
        private int growthPotential;
        private int entryBarrier;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PainPoint {
        private String title;
        private String severity;     // high / medium / low
        private String description;
        private List<String> evidence;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Opportunity {
        private String name;
        private String type;         // high_value / blue_ocean / differentiation
        private String description;
        private List<String> tags;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Risk {
        private String name;
        private String level;        // high / medium / low
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionItem {
        private String title;
        private String description;
    }
}
