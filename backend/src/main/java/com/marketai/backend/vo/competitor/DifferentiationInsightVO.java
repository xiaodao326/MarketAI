package com.marketai.backend.vo.competitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifferentiationInsightVO {
    /** core_opportunity / pricing_strategy / warning */
    private String type;
    private String title;
    private String description;
}
