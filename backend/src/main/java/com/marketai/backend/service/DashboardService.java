package com.marketai.backend.service;

import com.marketai.backend.vo.dashboard.AnomalyVO;
import com.marketai.backend.vo.dashboard.KeywordRankVO;
import com.marketai.backend.vo.dashboard.MetricsVO;
import com.marketai.backend.vo.dashboard.TrendDataVO;

import java.util.List;

public interface DashboardService {

    MetricsVO getMetrics(Long projectId, String timeRange);

    TrendDataVO getTrend(Long projectId, String timeRange, String metrics);

    List<KeywordRankVO> getTopKeywords(Long projectId, int limit);

    List<AnomalyVO> getAnomalies(Long projectId, int limit, String severity);
}
