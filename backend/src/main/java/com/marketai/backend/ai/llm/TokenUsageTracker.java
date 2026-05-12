package com.marketai.backend.ai.llm;

import com.marketai.backend.ai.llm.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token 消耗追踪器
 * 数据存储在 Redis HASH, 保留 30 天滚动窗口
 *
 * Key 设计:
 *   token:user:{userId}:{yyyy-MM-dd}    -> {prompt, completion, calls}
 *   token:project:{projectId}:{yyyy-MM-dd} -> {prompt, completion, calls}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageTracker {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final long TTL_DAYS = 30;

    private final StringRedisTemplate redisTemplate;

    /**
     * 记录一次 AI 调用的 token 消耗
     * @param userId    发起调用的用户 ID
     * @param projectId 关联项目 ID (可为 null)
     * @param response  AI 返回的响应 (含 token 统计)
     */
    public void record(Long userId, Long projectId, ChatResponse response) {
        if (response == null) return;
        String date = LocalDate.now().format(DATE_FMT);

        record(userKey(userId, date), response);
        if (projectId != null) {
            record(projectKey(projectId, date), response);
        }
    }

    /** 查询用户在指定日期的 token 消耗统计 */
    public Map<String, Object> getUserUsage(Long userId, LocalDate date) {
        return getUsage(userKey(userId, date.format(DATE_FMT)));
    }

    /** 查询项目在指定日期的 token 消耗统计 */
    public Map<String, Object> getProjectUsage(Long projectId, LocalDate date) {
        return getUsage(projectKey(projectId, date.format(DATE_FMT)));
    }

    // ── 内部方法 ──────────────────────────────────────────────

    private void record(String key, ChatResponse resp) {
        try {
            var hash = redisTemplate.opsForHash();
            hash.increment(key, "prompt_tokens", safeInt(resp.getPromptTokens()));
            hash.increment(key, "completion_tokens", safeInt(resp.getCompletionTokens()));
            hash.increment(key, "total_tokens", safeInt(resp.getTotalTokens()));
            hash.increment(key, "calls", 1L);
            redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            // token 统计失败不影响主流程
            log.warn("[TokenTracker] 记录失败: key={}, error={}", key, e.getMessage());
        }
    }

    private Map<String, Object> getUsage(String key) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        Map<String, Object> result = new HashMap<>();
        result.put("prompt_tokens", parseLong(raw.get("prompt_tokens")));
        result.put("completion_tokens", parseLong(raw.get("completion_tokens")));
        result.put("total_tokens", parseLong(raw.get("total_tokens")));
        result.put("calls", parseLong(raw.get("calls")));
        return result;
    }

    private static String userKey(Long userId, String date) {
        return "token:user:" + userId + ":" + date;
    }

    private static String projectKey(Long projectId, String date) {
        return "token:project:" + projectId + ":" + date;
    }

    private static long safeInt(Integer v) {
        return v != null ? v : 0L;
    }

    private static long parseLong(Object v) {
        if (v == null) return 0L;
        try { return Long.parseLong(String.valueOf(v)); } catch (NumberFormatException e) { return 0L; }
    }
}
