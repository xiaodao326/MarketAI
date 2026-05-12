package com.marketai.backend.ai.datasource;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 采集调度器
 * - 每 6 小时定时全量采集 (cron 表达式: 每天 00:00/06:00/12:00/18:00)
 * - 应用启动后异步触发一次初始采集 (PostConstruct 用虚拟线程, 不阻塞启动)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorScheduler {

    private final CollectorService collectorService;

    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledCollect() {
        log.info("[调度] 定时采集触发");
        collectorService.collectAll();
    }

    @PostConstruct
    public void initCollect() {
        // 虚拟线程异步执行, 避免阻塞 Spring 上下文初始化
        Thread.ofVirtual().name("collector-init").start(() -> {
            log.info("[调度] 启动初始采集开始");
            collectorService.collectAll();
            log.info("[调度] 启动初始采集完成");
        });
    }
}
