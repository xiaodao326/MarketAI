package com.marketai.backend.dto.competitor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "编辑单个竞品 — 所有字段可选,只更新非 null 字段")
@Data
public class UpdateCompetitorRequest {

    @Size(max = 100)
    @Schema(description = "竞品名称", example = "Zendesk")
    private String name;

    @Size(max = 500)
    @Schema(description = "竞品 logo URL")
    private String logoUrl;

    @Size(max = 500)
    @Schema(description = "竞品官网地址")
    private String website;

    @Schema(description = "竞品类型: direct/indirect/potential", example = "direct")
    private String type;

    @Schema(description = "所在区域/国家", example = "美国")
    private String region;

    @Schema(description = "用户综合评分 0.00-5.00", example = "4.5")
    private BigDecimal rating;

    @Schema(description = "月活跃用户数(估算)", example = "500000")
    private Integer mau;

    @Schema(description = "定价模式", example = "订阅制")
    private String pricingModel;

    @Schema(description = "融资阶段", example = "已上市")
    private String fundingStage;

    @Schema(description = "威胁等级: high/medium/low", example = "high")
    private String threatLevel;

    @Schema(description = "功能支持矩阵: feature -> yes/partial/no")
    private Map<String, String> features;

    @Schema(description = "AI 总结的优势列表")
    private List<String> strengths;

    @Schema(description = "AI 总结的劣势列表")
    private List<String> weaknesses;

    @Schema(description = "AI 不熟悉该竞品时为 true")
    private Boolean needsUserInput;
}
