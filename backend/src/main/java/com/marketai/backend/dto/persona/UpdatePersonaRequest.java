package com.marketai.backend.dto.persona;

import com.marketai.backend.ai.persona.dto.PersonaResult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "编辑画像 — 所有字段可选,只更新非空字段")
@Data
public class UpdatePersonaRequest {

    @Size(max = 50)
    @Schema(description = "画像姓名/代号", example = "小王")
    private String name;

    @Size(max = 100)
    @Schema(description = "职位/身份描述", example = "中小企业CTO")
    private String role;

    @Size(max = 20)
    @Schema(description = "年龄区间", example = "30-40")
    private String ageRange;

    @Schema(description = "市场占比百分比", example = "25.5")
    private BigDecimal marketShare;

    @Schema(description = "是否核心用户", example = "true")
    private Boolean isPrimary;

    @Schema(description = "核心目标标签")
    private List<String> goals;

    @Schema(description = "痛点列表")
    private List<PersonaResult.PainPoint> painPoints;

    @Size(max = 500)
    @Schema(description = "代表性用户引言")
    private String quote;

    @Schema(description = "决策参数")
    private PersonaResult.DecisionParams decisionParams;
}
