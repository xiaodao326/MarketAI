package com.marketai.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.dto.dashboard.DailyMetricDTO;
import com.marketai.backend.dto.dashboard.KeywordRankDTO;
import com.marketai.backend.mapper.DashboardMapper;
import com.marketai.backend.service.AnomalyDetector;
import com.marketai.backend.service.DashboardService;
import com.marketai.backend.vo.dashboard.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final AnomalyDetector anomalyDetector;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final long    CACHE_TTL = 600; // 10 分钟
    private static final ZoneId  SH        = ZoneId.of("Asia/Shanghai");

    // ── 对外接口 ──────────────────────────────────────────────

    @Override
    public MetricsVO getMetrics(Long projectId, String timeRange) {
        String key = "dashboard:" + projectId + ":metrics:" + timeRange;
        return cache(key, new TypeReference<>() {}, () -> computeMetrics(projectId, timeRange));
    }

    @Override
    public TrendDataVO getTrend(Long projectId, String timeRange, String metrics) {
        // 排序 metrics 参数保证相同内容不同顺序命中同一缓存
        String metricsKey = metrics == null ? "all"
            : Arrays.stream(metrics.split(",")).sorted().collect(Collectors.joining(","));
        String key = "dashboard:" + projectId + ":trend:" + timeRange + ":" + metricsKey;
        return cache(key, new TypeReference<>() {}, () -> computeTrend(projectId, timeRange, metrics));
    }

    @Override
    public List<KeywordRankVO> getTopKeywords(Long projectId, int limit) {
        String key = "dashboard:" + projectId + ":keywords:" + limit;
        return cache(key, new TypeReference<>() {}, () -> computeTopKeywords(projectId, limit));
    }

    @Override
    public List<AnomalyVO> getAnomalies(Long projectId, int limit, String severity) {
        String key = "dashboard:" + projectId + ":anomalies:" + limit + ":"
            + (severity == null ? "all" : severity);
        return cache(key, new TypeReference<>() {}, () -> computeAnomalies(projectId, limit, severity));
    }

    // ── 计算逻辑 ──────────────────────────────────────────────

    private MetricsVO computeMetrics(Long projectId, String timeRange) {
        int days = parseDays(timeRange);
        LocalDate today     = LocalDate.now(SH);
        LocalDate curStart  = today.minusDays(days - 1L);
        LocalDate prevEnd   = curStart.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1L);

        BigDecimal curSearch  = dashboardMapper.sumValue(projectId, "baidu_index", "search_volume",    curStart, today);
        BigDecimal prevSearch = dashboardMapper.sumValue(projectId, "baidu_index", "search_volume",    prevStart, prevEnd);
        BigDecimal curSocial  = dashboardMapper.sumValue(projectId, "weibo",       "social_mentions", curStart, today);
        BigDecimal prevSocial = dashboardMapper.sumValue(projectId, "weibo",       "social_mentions", prevStart, prevEnd);

        long curKw  = dashboardMapper.countDistinctKeywords(projectId, curStart, today);
        long prevKw = dashboardMapper.countDistinctKeywords(projectId, prevStart, prevEnd);

        // sentimentScore — Phase 1 代理指标: 基于搜索热度涨幅推算
        double searchPct      = pctChange(curSearch, prevSearch);
        long   curSentiment   = clamp(50 + Math.round(searchPct / 2), 0, 100);
        long   prevSentiment  = 50L;

        return MetricsVO.builder()
            .searchHeatIndex(pctCard(curSearch.longValue(), searchPct))
            .socialMentions(pctCard(curSocial.longValue(), pctChange(curSocial, prevSocial)))
            .competitorActiveCount(absCard(curKw, curKw - prevKw))
            .sentimentScore(absCard(curSentiment, curSentiment - prevSentiment))
            .build();
    }

    private TrendDataVO computeTrend(Long projectId, String timeRange, String metrics) {
        int days = parseDays(timeRange);
        LocalDate today     = LocalDate.now(SH);
        LocalDate startDate = today.minusDays(days - 1L);

        Set<String> requested = parseMetrics(metrics);

        // 生成连续日期列表（含空洞日期，值填 0）
        List<String> dates = startDate.datesUntil(today.plusDays(1))
            .map(LocalDate::toString)
            .collect(Collectors.toList());

        List<DailyMetricDTO> rawData = dashboardMapper.selectDailyTotals(projectId, startDate, today);

        // 构建查找表: "date|source|metricType" -> 合计值
        Map<String, Long> lookup = new HashMap<>();
        for (DailyMetricDTO dto : rawData) {
            String k = dto.getDataDate() + "|" + dto.getSource() + "|" + dto.getMetricType();
            lookup.merge(k, dto.getDailyTotal().longValue(), Long::sum);
        }

        List<TrendSeriesVO> series = new ArrayList<>();

        if (requested.contains("search")) {
            series.add(TrendSeriesVO.builder()
                .name("搜索热度")
                .data(dates.stream()
                    .map(d -> lookup.getOrDefault(d + "|baidu_index|search_volume", 0L))
                    .collect(Collectors.toList()))
                .yaxisIndex(0).build());
        }

        if (requested.contains("social")) {
            series.add(TrendSeriesVO.builder()
                .name("社媒讨论")
                .data(dates.stream()
                    .map(d -> lookup.getOrDefault(d + "|weibo|social_mentions", 0L))
                    .collect(Collectors.toList()))
                .yaxisIndex(0).build());
        }

        if (requested.contains("sentiment")) {
            // 情感值 = 归一化搜索量映射到 [40, 100]，使用独立 Y 轴
            List<Long> searchVals = dates.stream()
                .map(d -> lookup.getOrDefault(d + "|baidu_index|search_volume", 0L))
                .collect(Collectors.toList());
            series.add(TrendSeriesVO.builder()
                .name("情感值")
                .data(deriveSentiment(searchVals))
                .yaxisIndex(1).build());
        }

        return TrendDataVO.builder().dates(dates).series(series).build();
    }

    private List<KeywordRankVO> computeTopKeywords(Long projectId, int limit) {
        LocalDate today     = LocalDate.now(SH);
        LocalDate curStart  = today.minusDays(6);
        LocalDate prevEnd   = curStart.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(6);

        List<KeywordRankDTO> raw = dashboardMapper.selectKeywordRanking(
            projectId, curStart, today, prevStart, prevEnd, limit);

        List<KeywordRankVO> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            KeywordRankDTO dto = raw.get(i);
            result.add(KeywordRankVO.builder()
                .rank(i + 1)
                .keyword(dto.getKeyword())
                .volume(dto.getCurrentVolume().longValue())
                .deltaPercent(round1(pctChange(dto.getCurrentVolume(), dto.getPrevVolume())))
                .build());
        }
        return result;
    }

    private List<AnomalyVO> computeAnomalies(Long projectId, int limit, String severity) {
        // 取近 30 天数据，提供足够的均值/趋势计算窗口
        LocalDate startDate = LocalDate.now(SH).minusDays(29);
        List<DailyMetricDTO> rawData = dashboardMapper.selectForAnomalyDetection(projectId, startDate);
        return anomalyDetector.detect(rawData, limit, severity);
    }

    // ── 工具方法 ──────────────────────────────────────────────

    /** 将搜索量列表归一化到情感值区间 [40, 100] */
    private List<Long> deriveSentiment(List<Long> searchVals) {
        long maxVal = searchVals.stream().mapToLong(Long::longValue).max().orElse(0);
        if (maxVal == 0) return searchVals.stream().map(v -> 50L).collect(Collectors.toList());
        return searchVals.stream()
            .map(v -> clamp(40 + v * 60 / maxVal, 0, 100))
            .collect(Collectors.toList());
    }

    private Set<String> parseMetrics(String metrics) {
        if (metrics == null || metrics.isBlank()) return Set.of("search", "social", "sentiment");
        return new HashSet<>(Arrays.asList(metrics.split(",")));
    }

    private int parseDays(String timeRange) {
        return switch (timeRange) {
            case "30d" -> 30;
            case "90d" -> 90;
            default    ->  7;
        };
    }

    /** 环比百分比变化 (%) */
    private double pctChange(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return current.subtract(prev)
            .divide(prev, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }

    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private long clamp(long val, long min, long max) {
        return Math.max(min, Math.min(max, val));
    }

    /** 百分比变化卡片 (delta = %) */
    private MetricCardVO pctCard(long current, double pct) {
        return MetricCardVO.builder()
            .current(current)
            .delta(round1(pct))
            .trend(pct > 1.0 ? "up" : pct < -1.0 ? "down" : "stable")
            .build();
    }

    /** 绝对变化卡片 (delta = 原始差值) */
    private MetricCardVO absCard(long current, long absDelta) {
        return MetricCardVO.builder()
            .current(current)
            .delta((double) absDelta)
            .trend(absDelta > 0 ? "up" : absDelta < 0 ? "down" : "stable")
            .build();
    }

    // ── Redis 缓存工具 ────────────────────────────────────────

    /**
     * 通用缓存读写: 优先读 Redis，未命中则计算后写入。
     * 读写失败均降级为直接计算，不影响主流程。
     */
    private <T> T cache(String key, TypeReference<T> typeRef, Supplier<T> supplier) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (Exception e) {
            log.warn("[Dashboard] 缓存读取失败: key={}, err={}", key, e.getMessage());
        }

        T value = supplier.get();

        try {
            redisTemplate.opsForValue().set(
                key, objectMapper.writeValueAsString(value), Duration.ofSeconds(CACHE_TTL));
        } catch (Exception e) {
            log.warn("[Dashboard] 缓存写入失败: key={}, err={}", key, e.getMessage());
        }

        return value;
    }
}
