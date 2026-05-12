package com.marketai.backend.ai.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采集到的趋势数据点, 由连接器填充后放入 Redis 队列
 * projectId 由 CollectorService 在入队前设置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataPoint {
    private Long projectId;
    private String keyword;
    /** 数据源: baidu_index / weibo */
    private String source;
    /** 指标类型: search_volume / social_mentions */
    private String metricType;
    private BigDecimal value;
    private LocalDate dataDate;
}
