package com.marketai.backend.ai.persona;

import com.marketai.backend.service.PersonaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 画像生成任务消费者 (与 InsightTaskConsumer 同构)
 *
 * 每 5 秒轮询, 最多同时启动 3 个虚拟线程并行处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaTaskConsumer {

    private static final int CONCURRENCY = 3;

    private final PersonaTaskQueue taskQueue;
    private final PersonaService   personaService;

    @Scheduled(fixedDelay = 5_000)
    public void dispatch() {
        for (int i = 0; i < CONCURRENCY; i++) {
            Long taskId = taskQueue.dequeue();
            if (taskId == null) break;

            final long tid = taskId;
            Thread.ofVirtual()
                    .name("persona-task-" + tid)
                    .start(() -> {
                        log.info("[PersonaConsumer] 开始处理任务: taskId={}", tid);
                        try {
                            personaService.processTask(tid);
                        } catch (Exception e) {
                            log.error("[PersonaConsumer] 任务处理异常: taskId={}, error={}", tid, e.getMessage(), e);
                        }
                    });
        }
    }
}
