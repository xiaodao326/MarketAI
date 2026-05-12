package com.marketai.backend.ai.datasource.impl;

import com.marketai.backend.ai.datasource.DataSourceConnector;
import com.marketai.backend.ai.datasource.dto.TrendDataPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 百度指数连接器 — Phase 1 Mock 实现
 *
 * TODO: 接入真实百度指数 API
 *   - 官方 API 需申请开发者权限: https://index.baidu.com/api
 *   - 认证方式: Cookie-based (BDUSS), 需定期刷新
 *   - 返回结果: 分词级别指数 + PC/Mobile 分段 + 城市分布
 *   - 替换入口: 删除此类, 在 fetch() 中调用真实 HTTP 接口即可
 *
 * 当前实现: 根据关键词哈希生成稳定可重现的模拟时序数据
 *   - 正弦波 (周期 30 天) 模拟市场周期性波动
 *   - 随机噪声 (±7.5%) 模拟真实数据抖动
 *   - 值域: 1000-50000
 */
@Slf4j
@Component
public class BaiduIndexConnector implements DataSourceConnector {

    @Override
    public String getSourceName() {
        return "baidu_index";
    }

    @Override
    public List<TrendDataPoint> fetch(String keyword, LocalDate startDate, LocalDate endDate) {
        log.debug("[百度指数] 生成 Mock 数据: keyword={}, {} -> {}", keyword, startDate, endDate);

        // 关键词哈希作为随机种子, 保证同关键词、同时间范围数据稳定可重现
        long seed = (long) keyword.hashCode() * 31L + 17L;
        Random rng = new Random(seed);

        double base = 10000 + (Math.abs(seed) % 30000);
        double amplitude = base * 0.3;

        List<TrendDataPoint> result = new ArrayList<>();
        LocalDate current = startDate;
        int dayIndex = 0;

        while (!current.isAfter(endDate)) {
            double sine = Math.sin(2 * Math.PI * dayIndex / 30.0);
            double noise = (rng.nextDouble() - 0.5) * 0.15 * base;
            double raw = base + amplitude * sine + noise;
            double value = Math.max(1000, Math.min(50000, raw));

            result.add(TrendDataPoint.builder()
                    .keyword(keyword)
                    .source(getSourceName())
                    .metricType("search_volume")
                    .value(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP))
                    .dataDate(current)
                    .build());

            current = current.plusDays(1);
            dayIndex++;
        }

        log.debug("[百度指数] 生成完成: {} 条数据", result.size());
        return result;
    }

    @Override
    public boolean healthCheck() {
        return true;
    }
}
