package com.marketai.backend.vo.competitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeCompetitorsResponse {
    private Long   taskId;
    private String status;            // "pending"
    private int    estimatedSeconds;  // ~60
}
