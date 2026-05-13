package com.marketai.backend.controller;

import com.marketai.backend.common.Result;
import com.marketai.backend.dto.competitor.AnalyzeCompetitorsRequest;
import com.marketai.backend.dto.competitor.UpdateCompetitorRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.service.CompetitorService;
import com.marketai.backend.vo.competitor.AnalyzeCompetitorsResponse;
import com.marketai.backend.vo.competitor.CompetitorTaskStatusVO;
import com.marketai.backend.vo.competitor.CompetitorVO;
import com.marketai.backend.vo.competitor.DifferentiationInsightVO;
import com.marketai.backend.vo.competitor.FeatureMatrixVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/competitors")
@RequiredArgsConstructor
@Tag(name = "竞品分析", description = "AI 生成竞品对比矩阵与差异化建议")
public class CompetitorController {

    private final CompetitorService competitorService;

    @PostMapping("/analyze")
    @Operation(summary = "触发竞品分析", description = "项目必须已配置 competitors 名单")
    public Result<AnalyzeCompetitorsResponse> analyze(
            @Valid @RequestBody AnalyzeCompetitorsRequest request,
            @AuthenticationPrincipal User user) {
        return Result.success(competitorService.createTask(request, user));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务状态")
    public Result<CompetitorTaskStatusVO> getTaskStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        return Result.success(competitorService.getTaskStatus(taskId, user));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "项目下所有竞品", description = "按威胁等级 high→medium→low 排序")
    public Result<List<CompetitorVO>> list(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return Result.success(competitorService.listProjectCompetitors(projectId, user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "单个竞品详情")
    public Result<CompetitorVO> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return Result.success(competitorService.getCompetitor(id, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新竞品信息", description = "部分字段更新, 只传需要修改的字段")
    public Result<CompetitorVO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompetitorRequest request,
            @AuthenticationPrincipal User user) {
        return Result.success(competitorService.updateCompetitor(id, request, user));
    }

    @GetMapping("/project/{projectId}/matrix")
    @Operation(summary = "项目的功能对比矩阵")
    public Result<FeatureMatrixVO> matrix(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return Result.success(competitorService.getFeatureMatrix(projectId, user));
    }

    @GetMapping("/project/{projectId}/insights")
    @Operation(summary = "项目的差异化建议")
    public Result<List<DifferentiationInsightVO>> insights(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return Result.success(competitorService.getInsights(projectId, user));
    }
}
