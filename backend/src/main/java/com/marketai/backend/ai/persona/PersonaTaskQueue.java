package com.marketai.backend.ai.persona;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 画像生成任务队列 (与 InsightTaskQueue 同构, 独立 key 防止串扰)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaTaskQueue {

    static final String QUEUE_KEY = "queue:persona_tasks";

    private final StringRedisTemplate redisTemplate;

    public void enqueue(Long taskId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(taskId));
        log.debug("[PersonaQueue] 任务入队: taskId={}", taskId);
    }

    public Long dequeue() {
        String val = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        return val == null ? null : Long.parseLong(val);
    }

    public Long size() {
        return redisTemplate.opsForList().size(QUEUE_KEY);
    }
}
