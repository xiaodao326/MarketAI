package com.marketai.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.insight.InsightAnalyzer;
import com.marketai.backend.ai.insight.InsightTaskQueue;
import com.marketai.backend.ai.insight.dto.InsightResult;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.insight.CreateInsightRequest;
import com.marketai.backend.entity.AnalysisTask;
import com.marketai.backend.entity.InsightReport;
import com.marketai.backend.entity.Project;
import com.marketai.backend.entity.User;
import com.marketai.backend.mapper.AnalysisTaskMapper;
import com.marketai.backend.mapper.InsightReportMapper;
import com.marketai.backend.mapper.ProjectMapper;
import com.marketai.backend.service.InsightService;
import com.marketai.backend.vo.insight.CreateInsightResponse;
import com.marketai.backend.vo.insight.InsightReportSummaryVO;
import com.marketai.backend.vo.insight.InsightReportVO;
import com.marketai.backend.vo.insight.TaskStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private static final int    MAX_ACTIVE_TASKS   = 3;
    private static final String TASK_TYPE          = "insight";
    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalysisTaskMapper  taskMapper;
    private final InsightReportMapper reportMapper;
    private final ProjectMapper       projectMapper;
    private final InsightTaskQueue    taskQueue;
    private final InsightAnalyzer     analyzer;
    private final ObjectMapper        objectMapper;

    // ── 对外接口 ──────────────────────────────────────────────

    @Override
    public CreateInsightResponse createTask(CreateInsightRequest req, User user) {
        // 1. 验证项目归属
        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        if (!project.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        // 2. 限流：每用户最多 3 个并发任务
        int active = taskMapper.countActiveTasks(user.getId(), TASK_TYPE);
        if (active >= MAX_ACTIVE_TASKS) throw new BusinessException(ResultCode.INSIGHT_TASK_LIMIT);

        // 3. 序列化输入参数
        String inputParams;
        try {
            inputParams = objectMapper.writeValueAsString(
                    Map.of("productDescription", req.getProductDescription(), "depth", req.getDepth()));
        } catch (JsonProcessingException e) {
            throw new BusinessException("参数序列化失败");
        }

        // 4. 持久化任务
        AnalysisTask task = AnalysisTask.builder()
                .userId(user.getId())
                .projectId(req.getProjectId())
                .taskType(TASK_TYPE)
                .status("pending")
                .progress(0)
                .inputParams(inputParams)
                .createdAt(LocalDateTime.now())
                .build();
        taskMapper.insert(task);

        // 5. 入队
        taskQueue.enqueue(task.getId());
        log.info("[InsightService] 任务已创建: taskId={}, userId={}, projectId={}", task.getId(), user.getId(), req.getProjectId());

        int estimated = switch (req.getDepth()) { case "lite" -> 30; case "full" -> 120; default -> 60; };
        return CreateInsightResponse.builder()
                .taskId(task.getId())
                .status("pending")
                .estimatedSeconds(estimated)
                .build();
    }

    @Override
    public TaskStatusVO getTaskStatus(Long taskId, User user) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ResultCode.INSIGHT_TASK_NOT_FOUND);
        if (!task.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        return TaskStatusVO.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .progress(task.getProgress())
                .resultId(task.getResultId())
                .errorMessage(task.getErrorMessage())
                .build();
    }

    @Override
    public InsightReportVO getReport(Long reportId, User user) {
        InsightReport report = reportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ResultCode.INSIGHT_REPORT_NOT_FOUND);

        // 验证项目归属
        Project project = projectMapper.selectById(report.getProjectId());
        if (project == null || !project.getUserId().equals(user.getId()))
            throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        return toReportVO(report);
    }

    @Override
    public Page<InsightReportSummaryVO> listProjectReports(Long projectId, int page, int size, User user) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        if (!project.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        Page<InsightReport> dbPage = reportMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<InsightReport>()
                        .eq(InsightReport::getProjectId, projectId)
                        .eq(InsightReport::getStatus, "completed")
                        .orderByDesc(InsightReport::getCreatedAt));

        Page<InsightReportSummaryVO> result = new Page<>(page, size, dbPage.getTotal());
        result.setRecords(dbPage.getRecords().stream().map(this::toSummaryVO).collect(Collectors.toList()));
        return result;
    }

    // ── 任务处理 (消费者调用) ──────────────────────────────────

    @Override
    public void processTask(Long taskId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("[InsightService] 任务不存在: taskId={}", taskId);
            return;
        }
        if (!"pending".equals(task.getStatus())) {
            log.warn("[InsightService] 任务状态不是 pending，跳过: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        // 标记 running
        task.setStatus("running");
        task.setProgress(10);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        try {
            // 加载项目数据
            Project project = projectMapper.selectById(task.getProjectId());
            if (project == null) throw new IllegalStateException("项目不存在: " + task.getProjectId());

            // 解析输入参数
            Map<String, String> params = objectMapper.readValue(task.getInputParams(), new TypeReference<>() {});
            String productDescription = params.get("productDescription");
            String depth              = params.getOrDefault("depth", "standard");

            task.setProgress(20);
            taskMapper.updateById(task);

            // 调用 AI 分析
            InsightAnalyzer.AnalysisOutput output = analyzer.analyze(
                    task.getProjectId(), productDescription,
                    project.getIndustry(), project.getTargetMarket(),
                    project.getKeywords() != null ? project.getKeywords() : List.of(),
                    depth);

            task.setProgress(80);
            taskMapper.updateById(task);

            // 保存报告
            InsightReport report = buildReport(task, project, output);
            reportMapper.insert(report);

            // 完成任务
            task.setStatus("completed");
            task.setProgress(100);
            task.setResultId(report.getId());
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            log.info("[InsightService] 任务完成: taskId={}, reportId={}, tokens={}", taskId, report.getId(), output.tokensUsed());

        } catch (Exception e) {
            log.error("[InsightService] 任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(truncate(e.getMessage(), 500));
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    // ── 私有工具 ──────────────────────────────────────────────

    private InsightReport buildReport(AnalysisTask task, Project project, InsightAnalyzer.AnalysisOutput output) {
        InsightResult r = output.result();
        try {
            return InsightReport.builder()
                    .projectId(task.getProjectId())
                    .title("需求洞察报告 · " + project.getName())
                    .status("completed")
                    .marketFitScore(r.getMarketFitScore())
                    .dimensions(objectMapper.writeValueAsString(r.getDimensions()))
                    .painPoints(objectMapper.writeValueAsString(r.getPainPoints()))
                    .opportunities(objectMapper.writeValueAsString(r.getOpportunities()))
                    .risks(objectMapper.writeValueAsString(r.getRisks()))
                    .actions(objectMapper.writeValueAsString(r.getActions()))
                    .aiModel(output.modelName())
                    .tokensUsed(output.tokensUsed())
                    .createdAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("报告序列化失败", e);
        }
    }

    private InsightReportVO toReportVO(InsightReport r) {
        return InsightReportVO.builder()
                .id(r.getId())
                .projectId(r.getProjectId())
                .title(r.getTitle())
                .marketFitScore(r.getMarketFitScore())
                .dimensions(parseJson(r.getDimensions(), InsightResult.Dimensions.class))
                .painPoints(parseJsonList(r.getPainPoints(), InsightResult.PainPoint.class))
                .opportunities(parseJsonList(r.getOpportunities(), InsightResult.Opportunity.class))
                .risks(parseJsonList(r.getRisks(), InsightResult.Risk.class))
                .actions(parseJsonList(r.getActions(), InsightResult.ActionItem.class))
                .aiModel(r.getAiModel())
                .tokensUsed(r.getTokensUsed())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : null)
                .completedAt(r.getCompletedAt() != null ? r.getCompletedAt().format(FMT) : null)
                .build();
    }

    private InsightReportSummaryVO toSummaryVO(InsightReport r) {
        return InsightReportSummaryVO.builder()
                .id(r.getId())
                .title(r.getTitle())
                .marketFitScore(r.getMarketFitScore())
                .aiModel(r.getAiModel())
                .tokensUsed(r.getTokensUsed())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : null)
                .completedAt(r.getCompletedAt() != null ? r.getCompletedAt().format(FMT) : null)
                .build();
    }

    private <T> T parseJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readValue(json, type); }
        catch (JsonProcessingException e) { return null; }
    }

    private <T> List<T> parseJsonList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) { return List.of(); }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
