package com.marketai.backend.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricCardVO {
    private long current;
    private double delta;
    private String trend; // "up" / "down" / "stable"
}
