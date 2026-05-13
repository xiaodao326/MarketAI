package com.marketai.backend.ai.persona.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI 返回的单个画像结构 (画像生成任务返回 List&lt;PersonaResult&gt;)
 * 与 prompt 模板中的 JSON schema 严格对应。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonaResult {

    private String name;
    private String role;
    private String ageRange;

    /** 0-100 的百分比 */
    private Integer marketShare;

    private Boolean isPrimary;

    private List<String> goals;

    private List<PainPoint> painPoints;

    private String quote;

    private DecisionParams decisionParams;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PainPoint {
        private String title;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecisionParams {
        /** high / medium / low */
        private String paymentWillingness;
        /** 形如 "1-2 周" / "2-4 周" / "2-6 月" */
        private String decisionCycle;
        /** 形如 "0.5-2 万/年" */
        private String budgetRange;
        /** high / medium / low */
        private String techCapability;
    }
}
