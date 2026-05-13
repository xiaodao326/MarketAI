package com.marketai.backend.vo.persona;

import com.marketai.backend.ai.persona.dto.PersonaResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaVO {

    private Long   id;
    private Long   projectId;
    private String name;
    private String role;
    private String ageRange;
    private BigDecimal marketShare;
    private Boolean    isPrimary;

    private List<String>                  goals;
    private List<PersonaResult.PainPoint> painPoints;
    private String                        quote;
    private PersonaResult.DecisionParams  decisionParams;

    private String createdAt;
    private String updatedAt;
}
