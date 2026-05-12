package com.marketai.backend.mapper;

import com.marketai.backend.dto.dashboard.DailyMetricDTO;
import com.marketai.backend.dto.dashboard.KeywordRankDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardMapper {

    BigDecimal sumValue(@Param("projectId") Long projectId,
                        @Param("source") String source,
                        @Param("metricType") String metricType,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

    long countDistinctKeywords(@Param("projectId") Long projectId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    List<DailyMetricDTO> selectDailyTotals(@Param("projectId") Long projectId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    List<KeywordRankDTO> selectKeywordRanking(@Param("projectId") Long projectId,
                                              @Param("currentStart") LocalDate currentStart,
                                              @Param("currentEnd") LocalDate currentEnd,
                                              @Param("prevStart") LocalDate prevStart,
                                              @Param("prevEnd") LocalDate prevEnd,
                                              @Param("limit") int limit);

    List<DailyMetricDTO> selectForAnomalyDetection(@Param("projectId") Long projectId,
                                                    @Param("startDate") LocalDate startDate);
}
