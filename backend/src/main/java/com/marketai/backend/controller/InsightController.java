package com.marketai.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marketai.backend.common.Result;
import com.marketai.backend.dto.insight.CreateInsightRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.service.InsightService;
import com.marketai.backend.vo.insight.CreateInsightResponse;
import com.marketai.backend.vo.insight.InsightReportSummaryVO;
import com.marketai.backend.vo.insight.InsightReportVO;
import com.marketai.backend.vo.insight.TaskStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
@Tag(name = "AI 需求洞察", description = "异步生成结构化市场分析报告")
public class InsightController {

    private final InsightService insightService;

    @PostMapping
    @Operation(summary = "创建分析任务", description = "立即返回 taskId，后台异步处理")
    public Result<CreateInsightResponse> create(
            @Valid @RequestBody CreateInsightRequest request,
            @AuthenticationPrincipal User user) {
        return Result.success(insightService.createTask(request, user));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务状态", description = "status=completed 时 resultId 指向洞察报告")
    public Result<TaskStatusVO> getTaskStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        return Result.success(insightService.getTaskStatus(taskId, user));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "获取完整洞察报告")
    public Result<InsightReportVO> getReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal User user) {
        return Result.success(insightService.getReport(reportId, user));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "项目历史报告列表", description = "按创建时间降序分页")
    public Result<Page<InsightReportSummaryVO>> listProjectReports(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal User user) {
        return Result.success(insightService.listProjectReports(projectId, page, size, user));
    }
}
