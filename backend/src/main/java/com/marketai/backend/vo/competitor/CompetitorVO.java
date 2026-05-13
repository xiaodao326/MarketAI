package com.marketai.backend.vo.competitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorVO {

    private Long   id;
    private Long   projectId;
    private String name;
    private String logoUrl;
    private String website;
    private String type;
    private String region;
    private BigDecimal rating;
    private Integer mau;
    private String pricingModel;
    private String fundingStage;
    private String threatLevel;

    private Map<String, String> features;
    private List<String>        strengths;
    private List<String>        weaknesses;
    private Boolean             needsUserInput;

    private String createdAt;
    private String updatedAt;
}
