package com.marketai.backend.dto.competitor;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 编辑单个竞品 — 所有字段可选,只更新非 null 字段
 */
@Data
public class UpdateCompetitorRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String website;

    /** direct / indirect / potential */
    private String type;

    private String region;

    private BigDecimal rating;

    private Integer mau;

    private String pricingModel;

    private String fundingStage;

    /** high / medium / low */
    private String threatLevel;

    /** 功能支持矩阵: feature -> yes/partial/no */
    private Map<String, String> features;

    private List<String> strengths;
    private List<String> weaknesses;

    private Boolean needsUserInput;
}
