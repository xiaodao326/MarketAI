package com.marketai.backend.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyMetricDTO {
    private LocalDate dataDate;
    private String keyword;
    private String source;
    private String metricType;
    private BigDecimal dailyTotal;
}
