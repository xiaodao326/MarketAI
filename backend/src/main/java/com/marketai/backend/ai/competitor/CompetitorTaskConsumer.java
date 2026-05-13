package com.marketai.backend.ai.competitor;

import com.marketai.backend.service.CompetitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitorTaskConsumer {

    private static final int CONCURRENCY = 3;

    private final CompetitorTaskQueue taskQueue;
    private final CompetitorService   competitorService;

    @Scheduled(fixedDelay = 5_000)
    public void dispatch() {
        for (int i = 0; i < CONCURRENCY; i++) {
            Long taskId = taskQueue.dequeue();
            if (taskId == null) break;

            final long tid = taskId;
            Thread.ofVirtual()
                    .name("competitor-task-" + tid)
                    .start(() -> {
                        log.info("[CompetitorConsumer] 开始处理任务: taskId={}", tid);
                        try {
                            competitorService.processTask(tid);
                        } catch (Exception e) {
                            log.error("[CompetitorConsumer] 任务处理异常: taskId={}, error={}", tid, e.getMessage(), e);
                        }
                    });
        }
    }
}
