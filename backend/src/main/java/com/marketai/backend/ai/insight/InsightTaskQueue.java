package com.marketai.backend.ai.insight;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 洞察分析任务队列
 * Producer: enqueue(taskId) 将任务 ID 推入 Redis List
 * Consumer: dequeue()       从左端弹出一个任务 ID
 *
 * 只存 taskId 而非完整任务数据, 消费时从 DB 加载最新状态,
 * 避免任务被修改/取消后队列仍持有旧数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightTaskQueue {

    static final String QUEUE_KEY = "queue:insight_tasks";

    private final StringRedisTemplate redisTemplate;

    public void enqueue(Long taskId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(taskId));
        log.debug("[InsightQueue] 任务入队: taskId={}", taskId);
    }

    /** @return taskId, 队列为空时返回 null */
    public Long dequeue() {
        String val = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        return val == null ? null : Long.parseLong(val);
    }

    public Long size() {
        return redisTemplate.opsForList().size(QUEUE_KEY);
    }
}
