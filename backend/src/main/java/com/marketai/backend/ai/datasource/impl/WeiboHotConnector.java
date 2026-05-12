package com.marketai.backend.ai.datasource.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.datasource.DataSourceConnector;
import com.marketai.backend.ai.datasource.dto.TrendDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 微博热搜连接器
 * 数据来源: https://github.com/justjavac/weibo-trending-hot-search (开源每日快照)
 * 数据格式: [[rank, title, hot_value, url, category], ...]
 * 匹配策略: 标题包含关键词 -> 取最高热度值作为 social_mentions 指标
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeiboHotConnector implements DataSourceConnector {

    private static final String BASE_URL =
            "https://raw.githubusercontent.com/justjavac/weibo-trending-hot-search/master/raw/";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_RETRIES = 3;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;

    @Override
    public String getSourceName() {
        return "weibo";
    }

    @Override
    public List<TrendDataPoint> fetch(String keyword, LocalDate startDate, LocalDate endDate) {
        log.info("[微博热搜] 开始采集: keyword={}, {} -> {}", keyword, startDate, endDate);
        List<TrendDataPoint> result = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            try {
                result.add(fetchSingleDay(keyword, current));
            } catch (Exception e) {
                log.warn("[微博热搜] 单日采集失败, 跳过: date={}, keyword={}, error={}",
                        current, keyword, e.getMessage());
            }
            current = current.plusDays(1);
        }

        log.info("[微博热搜] 采集完成: keyword={}, {} 条数据", keyword, result.size());
        return result;
    }

    private TrendDataPoint fetchSingleDay(String keyword, LocalDate date) throws Exception {
        String url = BASE_URL + date.format(DATE_FMT) + ".json";
        String body = fetchWithRetry(url);

        List<List<Object>> items = objectMapper.readValue(body, new TypeReference<>() {});

        long maxHot = 0;
        for (List<Object> item : items) {
            if (item.size() < 3) continue;
            String title = String.valueOf(item.get(1));
            if (title.contains(keyword)) {
                Object hotObj = item.get(2);
                long hot = hotObj instanceof Number n ? n.longValue()
                        : Long.parseLong(String.valueOf(hotObj));
                if (hot > maxHot) maxHot = hot;
            }
        }

        // maxHot=0 表示当日该关键词未上榜, 仍记录为 0 便于分析趋势缺口
        return TrendDataPoint.builder()
                .keyword(keyword)
                .source(getSourceName())
                .metricType("social_mentions")
                .value(BigDecimal.valueOf(maxHot))
                .dataDate(date)
                .build();
    }

    /**
     * 带指数退避的 HTTP GET, 最多重试 MAX_RETRIES 次
     * 退避时间: 1s -> 2s -> 4s
     * 404 表示该日期快照尚未生成, 直接抛出 (不计入重试次数)
     */
    private String fetchWithRetry(String url) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse resp = HttpRequest.get(url).timeout(TIMEOUT_MS).execute();
                if (resp.getStatus() == 404) {
                    throw new IllegalStateException("404: 快照文件不存在 " + url);
                }
                if (resp.getStatus() != 200) {
                    throw new IllegalStateException("HTTP " + resp.getStatus() + ": " + url);
                }
                return resp.body();
            } catch (IllegalStateException e) {
                // 404 / 非 200 不重试, 直接上抛
                throw e;
            } catch (Exception e) {
                last = e;
                if (attempt < MAX_RETRIES) {
                    long backoff = (1L << (attempt - 1)) * 1000;
                    log.warn("[微博热搜] 第 {}/{} 次请求失败, {}ms 后重试: url={}, error={}",
                            attempt, MAX_RETRIES, backoff, url, e.getMessage());
                    Thread.sleep(backoff);
                }
            }
        }
        throw last;
    }

    @Override
    public boolean healthCheck() {
        try {
            String url = BASE_URL + LocalDate.now().minusDays(2).format(DATE_FMT) + ".json";
            String body = HttpRequest.get(url).timeout(5000).execute().body();
            return body != null && body.startsWith("[");
        } catch (Exception e) {
            log.warn("[微博热搜] 健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
