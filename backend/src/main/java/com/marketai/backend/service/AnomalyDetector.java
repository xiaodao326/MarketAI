package com.marketai.backend.service;

import com.marketai.backend.dto.dashboard.DailyMetricDTO;
import com.marketai.backend.vo.dashboard.AnomalyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 市场异动检测器
 * 规则:
 *   critical    — 单日值 > 近 7 日均值的 2 倍
 *   warning     — 连续 3 天下降 > 8%
 *   opportunity — 最新值突破历史峰值 20%
 */
@Slf4j
@Component
public class AnomalyDetector {

    private static final double CRITICAL_MULT    = 2.0;
    private static final double WARNING_DROP     = 0.08;  // 8%
    private static final double OPPORTUNITY_RISE = 0.20;  // 20%

    public List<AnomalyVO> detect(List<DailyMetricDTO> rawData, int limit, String severity) {
        // 按 keyword|source 分组，组内按日期升序
        Map<String, List<DailyMetricDTO>> groups = rawData.stream().collect(
            Collectors.groupingBy(
                d -> d.getKeyword() + "|" + d.getSource(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> { list.sort(Comparator.comparing(DailyMetricDTO::getDataDate)); return list; }
                )
            )
        );

        List<AnomalyVO> all = new ArrayList<>();
        for (Map.Entry<String, List<DailyMetricDTO>> entry : groups.entrySet()) {
            String[] parts   = entry.getKey().split("\\|", 2);
            String keyword   = parts[0];
            String source    = parts[1];
            List<DailyMetricDTO> series = entry.getValue();
            if (series.size() < 4) continue;

            all.addAll(detectCritical(keyword, source, series));
            all.addAll(detectWarning(keyword, source, series));
            all.addAll(detectOpportunity(keyword, source, series));
        }

        return all.stream()
            .filter(a -> severity == null || severity.equals(a.getSeverity()))
            .sorted(Comparator.comparing(AnomalyVO::getOccurredAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ── critical: 单日值 > 7 日均值的 2 倍 ────────────────────

    private List<AnomalyVO> detectCritical(String keyword, String source,
                                            List<DailyMetricDTO> series) {
        List<AnomalyVO> result = new ArrayList<>();
        for (int i = 7; i < series.size(); i++) {
            double cur  = series.get(i).getDailyTotal().doubleValue();
            double avg7 = series.subList(i - 7, i).stream()
                .mapToDouble(d -> d.getDailyTotal().doubleValue()).average().orElse(0);
            if (avg7 > 0 && cur > avg7 * CRITICAL_MULT) {
                result.add(AnomalyVO.builder()
                    .id(makeId("C", keyword, source, series.get(i).getDataDate()))
                    .severity("critical")
                    .title(String.format("「%s」热度异常飙升", keyword))
                    .description(String.format("当日值 %.0f，是近 7 日均值 %.0f 的 %.1f 倍",
                        cur, avg7, cur / avg7))
                    .source(source)
                    .occurredAt(series.get(i).getDataDate())
                    .build());
            }
        }
        return result;
    }

    // ── warning: 连续 3 天每日跌幅 > 8% ─────────────────────────

    private List<AnomalyVO> detectWarning(String keyword, String source,
                                           List<DailyMetricDTO> series) {
        List<AnomalyVO> result = new ArrayList<>();
        for (int i = 2; i < series.size(); i++) {
            double v0 = series.get(i - 2).getDailyTotal().doubleValue();
            double v1 = series.get(i - 1).getDailyTotal().doubleValue();
            double v2 = series.get(i).getDailyTotal().doubleValue();
            if (v0 > 0 && v1 > 0 && v2 > 0
                    && (v1 - v0) / v0 < -WARNING_DROP
                    && (v2 - v1) / v1 < -WARNING_DROP) {
                double totalDrop = (v2 - v0) / v0 * 100;
                result.add(AnomalyVO.builder()
                    .id(makeId("W", keyword, source, series.get(i).getDataDate()))
                    .severity("warning")
                    .title(String.format("「%s」连续 3 日下滑预警", keyword))
                    .description(String.format("连续 3 天每日跌幅超 %.0f%%，累计下降 %.1f%%",
                        WARNING_DROP * 100, Math.abs(totalDrop)))
                    .source(source)
                    .occurredAt(series.get(i).getDataDate())
                    .build());
            }
        }
        return result;
    }

    // ── opportunity: 最新值突破历史峰值 20% ──────────────────────

    private List<AnomalyVO> detectOpportunity(String keyword, String source,
                                               List<DailyMetricDTO> series) {
        int last = series.size() - 1;
        if (last < 7) return Collections.emptyList();

        double latest       = series.get(last).getDailyTotal().doubleValue();
        double historicalMax = series.subList(0, last).stream()
            .mapToDouble(d -> d.getDailyTotal().doubleValue()).max().orElse(0);

        if (historicalMax > 0 && latest > historicalMax * (1 + OPPORTUNITY_RISE)) {
            double riseRate = (latest - historicalMax) / historicalMax * 100;
            return List.of(AnomalyVO.builder()
                .id(makeId("O", keyword, source, series.get(last).getDataDate()))
                .severity("opportunity")
                .title(String.format("「%s」搜索量突破历史峰值", keyword))
                .description(String.format("当前值 %.0f 超越历史峰值 %.0f，涨幅 %.1f%%",
                    latest, historicalMax, riseRate))
                .source(source)
                .occurredAt(series.get(last).getDataDate())
                .build());
        }
        return Collections.emptyList();
    }

    private String makeId(String type, String keyword, String source, LocalDate date) {
        String raw = type + keyword + source + date;
        return type.toLowerCase() + Integer.toUnsignedString(raw.hashCode());
    }
}
