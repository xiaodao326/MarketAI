package com.marketai.backend.ai.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.datasource.dto.TrendDataPoint;
import com.marketai.backend.entity.TrendData;
import com.marketai.backend.mapper.TrendDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 趋势数据队列
 * Producer: enqueue() 将 TrendDataPoint 序列化后 push 到 Redis List
 * Consumer: 每 30 秒批量 pop 最多 100 条, 通过 INSERT IGNORE 批量写入 MySQL
 *
 * Redis key: queue:trend_data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorQueue {

    private static final String QUEUE_KEY = "queue:trend_data";
    private static final int BATCH_SIZE = 100;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TrendDataMapper trendDataMapper;

    public void enqueue(TrendDataPoint point) {
        try {
            String json = objectMapper.writeValueAsString(point);
            redisTemplate.opsForList().rightPush(QUEUE_KEY, json);
        } catch (Exception e) {
            log.error("[队列] 入队失败: keyword={}, error={}", point.getKeyword(), e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void consume() {
        List<String> batch = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            String item = redisTemplate.opsForList().leftPop(QUEUE_KEY);
            if (item == null) break;
            batch.add(item);
        }
        if (batch.isEmpty()) return;

        log.debug("[队列] 出队 {} 条, 开始批量写入", batch.size());

        List<TrendData> entities = batch.stream()
                .map(json -> {
                    try {
                        TrendDataPoint p = objectMapper.readValue(json, TrendDataPoint.class);
                        return TrendData.builder()
                                .projectId(p.getProjectId())
                                .keyword(p.getKeyword())
                                .source(p.getSource())
                                .metricType(p.getMetricType())
                                .value(p.getValue())
                                .dataDate(p.getDataDate())
                                .createdAt(LocalDateTime.now())
                                .build();
                    } catch (Exception e) {
                        log.error("[队列] 反序列化失败: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        if (!entities.isEmpty()) {
            try {
                trendDataMapper.insertIgnoreBatch(entities);
                log.info("[队列] 批量写入完成: {} 条趋势数据", entities.size());
            } catch (Exception e) {
                log.error("[队列] 批量写入失败: {}", e.getMessage(), e);
            }
        }
    }
}
