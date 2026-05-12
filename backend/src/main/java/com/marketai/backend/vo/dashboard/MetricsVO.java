package com.marketai.backend.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsVO {
    private MetricCardVO searchHeatIndex;
    private MetricCardVO socialMentions;
    private MetricCardVO competitorActiveCount;
    private MetricCardVO sentimentScore;
}
