package com.marketai.backend.controller;

import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.Result;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.entity.Project;
import com.marketai.backend.entity.User;
import com.marketai.backend.mapper.ProjectMapper;
import com.marketai.backend.service.DashboardService;
import com.marketai.backend.vo.dashboard.AnomalyVO;
import com.marketai.backend.vo.dashboard.KeywordRankVO;
import com.marketai.backend.vo.dashboard.MetricsVO;
import com.marketai.backend.vo.dashboard.TrendDataVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "市场趋势仪表盘")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProjectMapper    projectMapper;

    private static final Set<String> VALID_RANGES     = Set.of("7d", "30d", "90d");
    private static final Set<String> VALID_SEVERITIES = Set.of("critical", "warning", "opportunity");

    @GetMapping("/{projectId}/metrics")
    @Operation(summary = "核心指标卡", description = "4 张指标卡: 搜索热度/社媒讨论/竞品活跃数/情感值")
    public Result<MetricsVO> getMetrics(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "7d") String timeRange,
            @AuthenticationPrincipal User user) {
        checkAccess(projectId, user);
        checkTimeRange(timeRange);
        return Result.success(dashboardService.getMetrics(projectId, timeRange));
    }

    @GetMapping("/{projectId}/trend")
    @Operation(summary = "时序趋势数据", description = "返回 ECharts 可直接渲染的 dates + series 结构")
    public Result<TrendDataVO> getTrend(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "7d") String timeRange,
            @RequestParam(required = false) String metrics,
            @AuthenticationPrincipal User user) {
        checkAccess(projectId, user);
        checkTimeRange(timeRange);
        return Result.success(dashboardService.getTrend(projectId, timeRange, metrics));
    }

    @GetMapping("/{projectId}/keywords/top")
    @Operation(summary = "热门关键词排行")
    public Result<List<KeywordRankVO>> getTopKeywords(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit,
            @AuthenticationPrincipal User user) {
        checkAccess(projectId, user);
        return Result.success(dashboardService.getTopKeywords(projectId, limit));
    }

    @GetMapping("/{projectId}/anomalies")
    @Operation(summary = "市场异动事件流", description = "基于趋势数据规则判断: critical/warning/opportunity")
    public Result<List<AnomalyVO>> getAnomalies(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) String severity,
            @AuthenticationPrincipal User user) {
        checkAccess(projectId, user);
        if (severity != null && !VALID_SEVERITIES.contains(severity)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return Result.success(dashboardService.getAnomalies(projectId, limit, severity));
    }

    // ── 内部校验 ──────────────────────────────────────────────

    private void checkAccess(Long projectId, User user) {
        if (user == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        if (!project.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);
    }

    private void checkTimeRange(String timeRange) {
        if (!VALID_RANGES.contains(timeRange)) throw new BusinessException(ResultCode.PARAM_ERROR);
    }
}
