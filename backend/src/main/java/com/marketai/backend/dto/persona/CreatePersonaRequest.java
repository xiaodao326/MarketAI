package com.marketai.backend.dto.persona;

import com.marketai.backend.ai.persona.dto.PersonaResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 手动创建画像的请求体 */
@Data
public class CreatePersonaRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String role;

    @NotBlank
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
