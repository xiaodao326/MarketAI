package com.marketai.backend.ai.llm;

import com.marketai.backend.ai.llm.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 Redis 的令牌桶限流器
 * 策略: 每个用户每分钟最多 N 次 AI 调用 (滑动计数器, 60s 自动过期)
 *
 * 用 Lua 脚本保证 INCR + EXPIRE 的原子性, 避免并发竞争下计数不准
 */
@Slf4j
@Component
public class RateLimiter {

    private static final String KEY_PREFIX = "rate_limit:ai:user:";

    // Lua 脚本: 原子性 INCR + 首次设置 60s 过期
    private static final RedisScript<Long> INCR_SCRIPT = RedisScript.of(
            "local count = redis.call('INCR', KEYS[1])\n" +
            "if count == 1 then\n" +
            "  redis.call('EXPIRE', KEYS[1], 60)\n" +
            "end\n" +
            "return count",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;

    public RateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${marketai.ai.rate-limit.requests-per-minute:10}") int requestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.requestsPerMinute = requestsPerMinute;
    }

    /**
     * 检查并消耗一个令牌, 超限时抛出 TooManyRequestsException
     * @param userId 当前用户 ID
     */
    public void check(Long userId) {
        String key = KEY_PREFIX + userId;
        Long count = redisTemplate.execute(INCR_SCRIPT, List.of(key));
        if (count != null && count > requestsPerMinute) {
            log.warn("[限流] 用户 {} AI 调用超限: {}/{}/分钟", userId, count, requestsPerMinute);
            throw new TooManyRequestsException(
                    "AI 调用频率超限, 每分钟最多 " + requestsPerMinute + " 次, 请稍后再试");
        }
    }

    /** 查询当前用户在当前时间窗口已使用的调用次数 */
    public long getCurrentCount(Long userId) {
        String val = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return val == null ? 0L : Long.parseLong(val);
    }
}
