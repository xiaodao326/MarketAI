package com.marketai.backend.ai.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketai.backend.ai.datasource.dto.TrendDataPoint;
import com.marketai.backend.entity.Project;
import com.marketai.backend.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 数据采集服务
 * - 注入所有 DataSourceConnector 实现 (Spring 自动收集)
 * - 同一项目的多关键词并发采集 (CompletableFuture)
 * - 单数据源失败不影响其他数据源
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorService {

    private final List<DataSourceConnector> connectors;
    private final ProjectMapper projectMapper;
    private final CollectorQueue collectorQueue;

    /**
     * 为指定项目的所有关键词采集最近 7 天的趋势数据
     * 各关键词并发执行, 汇总后统一入队
     */
    public void collectForProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            log.warn("[采集] 项目不存在: projectId={}", projectId);
            return;
        }
        List<String> keywords = project.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            log.info("[采集] 项目无关键词, 跳过: projectId={}", projectId);
            return;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // 最近 7 天
        LocalDateTime t0 = LocalDateTime.now();
        log.info("[采集] 开始: projectId={}, keywords={}, {} -> {}", projectId, keywords, startDate, endDate);

        List<CompletableFuture<Void>> futures = keywords.stream()
                .map(kw -> CompletableFuture.runAsync(
                        () -> collectKeyword(projectId, kw, startDate, endDate)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("[采集] 完成: projectId={}, 耗时 {}ms",
                projectId, Duration.between(t0, LocalDateTime.now()).toMillis());
    }

    /**
     * 为所有活跃项目 (status=1) 采集数据, 逐项目顺序执行
     * 单项目采集异常不影响后续项目
     */
    public void collectAll() {
        List<Project> active = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getStatus, 1));
        log.info("[采集] 全量采集开始: {} 个活跃项目", active.size());
        for (Project p : active) {
            try {
                collectForProject(p.getId());
            } catch (Exception e) {
                log.error("[采集] 项目异常: projectId={}, error={}", p.getId(), e.getMessage(), e);
            }
        }
        log.info("[采集] 全量采集完成");
    }

    /** 单关键词采集所有已注册数据源, 结果入 Redis 队列 */
    private void collectKeyword(Long projectId, String keyword, LocalDate startDate, LocalDate endDate) {
        for (DataSourceConnector connector : connectors) {
            try {
                log.debug("[采集] source={}, keyword={}", connector.getSourceName(), keyword);
                List<TrendDataPoint> points = connector.fetch(keyword, startDate, endDate);
                points.forEach(p -> {
                    p.setProjectId(projectId);
                    collectorQueue.enqueue(p);
                });
                log.info("[采集] source={}, keyword={}, {} 条数据已入队",
                        connector.getSourceName(), keyword, points.size());
            } catch (Exception e) {
                log.error("[采集] 数据源失败: source={}, keyword={}, error={}",
                        connector.getSourceName(), keyword, e.getMessage(), e);
            }
        }
    }
}
