package com.marketai.backend.dto.persona;

import com.marketai.backend.ai.persona.dto.PersonaResult;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 编辑画像的请求体 — 所有字段可选,只更新非空字段
 * 用于前端"失焦自动保存"场景,通常一次只传一个改动字段
 */
@Data
public class UpdatePersonaRequest {

    @Size(max = 50)
    private String name;

    @Size(max = 100)
    private String role;

    @Size(max = 20)
    private String ageRange;

    private BigDecimal marketShare;

    private Boolean isPrimary;

    private List<String> goals;

    private List<PersonaResult.PainPoint> painPoints;

    @Size(max = 500)
    private String quote;

    private PersonaResult.DecisionParams decisionParams;
}
