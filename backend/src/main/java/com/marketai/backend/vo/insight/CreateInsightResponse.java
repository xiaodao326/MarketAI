package com.marketai.backend.vo.insight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInsightResponse {
    private Long   taskId;
    private String status;           // "pending"
    private int    estimatedSeconds; // lite=30, standard=60, full=120
}
