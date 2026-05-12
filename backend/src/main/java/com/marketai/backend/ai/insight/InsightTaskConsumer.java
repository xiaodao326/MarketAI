package com.marketai.backend.ai.insight;

import com.marketai.backend.service.InsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 洞察分析任务消费者
 *
 * 每 5 秒轮询 Redis 队列, 最多同时启动 3 个虚拟线程并行处理任务。
 * 使用虚拟线程而非线程池, 因 AI API 调用是纯 I/O 阻塞, 虚拟线程成本极低。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightTaskConsumer {

    private static final int CONCURRENCY = 3;

    private final InsightTaskQueue taskQueue;
    private final InsightService   insightService;

    @Scheduled(fixedDelay = 5_000)
    public void dispatch() {
        for (int i = 0; i < CONCURRENCY; i++) {
            Long taskId = taskQueue.dequeue();
            if (taskId == null) break;

            final long tid = taskId;
            Thread.ofVirtual()
                    .name("insight-task-" + tid)
                    .start(() -> {
                        log.info("[InsightConsumer] 开始处理任务: taskId={}", tid);
                        try {
                            insightService.processTask(tid);
                        } catch (Exception e) {
                            log.error("[InsightConsumer] 任务处理异常: taskId={}, error={}", tid, e.getMessage(), e);
                        }
                    });
        }
    }
}
