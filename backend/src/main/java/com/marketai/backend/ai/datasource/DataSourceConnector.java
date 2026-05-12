package com.marketai.backend.ai.datasource;

import com.marketai.backend.ai.datasource.dto.TrendDataPoint;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据源连接器接口
 * 每个数据源独立实现此接口, 通过 Spring 自动注入到 CollectorService
 */
public interface DataSourceConnector {

    /** 数据源唯一标识, 对应 trend_data.source 字段 */
    String getSourceName();

    /**
     * 采集指定关键词在时间范围内的趋势数据
     * @param keyword   监控关键词
     * @param startDate 起始日期 (含)
     * @param endDate   结束日期 (含)
     * @return 按日粒度的趋势数据点列表, projectId 由调用方填充
     */
    List<TrendDataPoint> fetch(String keyword, LocalDate startDate, LocalDate endDate);

    /** 健康检查: 验证数据源是否可正常访问 */
    boolean healthCheck();
}
