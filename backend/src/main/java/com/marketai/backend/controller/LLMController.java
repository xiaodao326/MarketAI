package com.marketai.backend.controller;

import com.marketai.backend.ai.llm.LLMFactory;
import com.marketai.backend.ai.llm.RateLimiter;
import com.marketai.backend.ai.llm.TokenUsageTracker;
import com.marketai.backend.ai.llm.dto.ChatRequest;
import com.marketai.backend.ai.llm.dto.ChatResponse;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.Result;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/llm")
@RequiredArgsConstructor
@Tag(name = "LLM", description = "AI 模型调用与 Token 消耗查询")
public class LLMController {

    private final LLMFactory llmFactory;
    private final RateLimiter rateLimiter;
    private final TokenUsageTracker tokenUsageTracker;

    @Value("${marketai.admin.emails:demo@marketai.com}")
    private String adminEmails;

    @PostMapping("/chat")
    @Operation(summary = "AI 对话 (仅管理员)", description = "测试端点, 向当前激活的 LLM 发送对话请求")
    public Result<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {
        checkAuth(user);
        checkAdmin(user);
        rateLimiter.check(user.getId());

        log.info("[LLM] chat 请求: userId={}, provider={}, msgCount={}",
                user.getId(), llmFactory.getActiveProvider(), request.getMessages().size());

        ChatResponse response = llmFactory.getProvider().chat(request);
        tokenUsageTracker.record(user.getId(), null, response);
        return Result.success(response);
    }

    @GetMapping("/usage")
    @Operation(summary = "查询 Token 消耗", description = "查询当前用户今日的 AI 调用次数和 Token 消耗")
    public Result<Map<String, Object>> usage(@AuthenticationPrincipal User user) {
        checkAuth(user);
        Map<String, Object> data = tokenUsageTracker.getUserUsage(user.getId(), LocalDate.now());
        data.put("remaining", Math.max(0L,
                10L - rateLimiter.getCurrentCount(user.getId())));
        data.put("provider", llmFactory.getActiveProvider());
        return Result.success(data);
    }

    @PostMapping("/switch")
    @Operation(summary = "切换 LLM 提供商 (仅管理员)")
    public Result<String> switchProvider(
            @RequestParam String provider,
            @AuthenticationPrincipal User user) {
        checkAuth(user);
        checkAdmin(user);
        llmFactory.switchProvider(provider);
        return Result.success("已切换至: " + provider);
    }

    @GetMapping("/providers")
    @Operation(summary = "查看可用 Provider 列表")
    public Result<List<String>> providers(@AuthenticationPrincipal User user) {
        checkAuth(user);
        return Result.success(List.copyOf(llmFactory.availableProviders()));
    }

    // ── 内部方法 ──────────────────────────────────────────────

    private void checkAuth(User user) {
        if (user == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    private void checkAdmin(User user) {
        List<String> admins = Arrays.asList(adminEmails.split(","));
        if (!admins.contains(user.getEmail().trim())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
