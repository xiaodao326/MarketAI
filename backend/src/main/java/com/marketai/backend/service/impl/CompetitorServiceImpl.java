package com.marketai.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.competitor.CompetitorAnalyzer;
import com.marketai.backend.ai.competitor.CompetitorTaskQueue;
import com.marketai.backend.ai.competitor.dto.CompetitorAnalysisResult;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.competitor.AnalyzeCompetitorsRequest;
import com.marketai.backend.dto.competitor.UpdateCompetitorRequest;
import com.marketai.backend.entity.AnalysisTask;
import com.marketai.backend.entity.Competitor;
import com.marketai.backend.entity.CompetitorReport;
import com.marketai.backend.entity.Project;
import com.marketai.backend.entity.User;
import com.marketai.backend.mapper.AnalysisTaskMapper;
import com.marketai.backend.mapper.CompetitorMapper;
import com.marketai.backend.mapper.CompetitorReportMapper;
import com.marketai.backend.mapper.ProjectMapper;
import com.marketai.backend.service.CompetitorService;
import com.marketai.backend.vo.competitor.AnalyzeCompetitorsResponse;
import com.marketai.backend.vo.competitor.CompetitorTaskStatusVO;
import com.marketai.backend.vo.competitor.CompetitorVO;
import com.marketai.backend.vo.competitor.DifferentiationInsightVO;
import com.marketai.backend.vo.competitor.FeatureMatrixVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitorServiceImpl implements CompetitorService {

    private static final int    MAX_ACTIVE_TASKS = 3;
    private static final String TASK_TYPE        = "competitor";
    private static final DateTimeFormatter FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalysisTaskMapper     taskMapper;
    private final CompetitorMapper       competitorMapper;
    private final CompetitorReportMapper reportMapper;
    private final ProjectMapper          projectMapper;
    private final CompetitorTaskQueue    taskQueue;
    private final CompetitorAnalyzer     analyzer;
    private final ObjectMapper           objectMapper;

    // ── 对外接口 ──────────────────────────────────────────────

    @Override
    public AnalyzeCompetitorsResponse createTask(AnalyzeCompetitorsRequest req, User user) {
        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        if (!project.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        // 关键前置: 项目必须配置过竞品名单
        if (project.getCompetitors() == null || project.getCompetitors().isEmpty()) {
            throw new BusinessException(ResultCode.COMPETITOR_LIST_EMPTY);
        }

        int active = taskMapper.countActiveTasks(user.getId(), TASK_TYPE);
        if (active >= MAX_ACTIVE_TASKS) throw new BusinessException(ResultCode.COMPETITOR_TASK_LIMIT);

        String inputParams;
        try {
            inputParams = objectMapper.writeValueAsString(Map.of("projectId", req.getProjectId()));
        } catch (JsonProcessingException e) {
            throw new BusinessException("参数序列化失败");
        }

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

        taskQueue.enqueue(task.getId());
        log.info("[CompetitorService] 任务已创建: taskId={}, userId={}, projectId={}, count={}",
                task.getId(), user.getId(), req.getProjectId(), project.getCompetitors().size());

        return AnalyzeCompetitorsResponse.builder()
                .taskId(task.getId())
                .status("pending")
                .estimatedSeconds(60)
                .build();
    }

    @Override
    public CompetitorTaskStatusVO getTaskStatus(Long taskId, User user) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ResultCode.COMPETITOR_TASK_NOT_FOUND);
        if (!task.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        return CompetitorTaskStatusVO.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .progress(task.getProgress())
                .projectId(task.getProjectId())
                .errorMessage(task.getErrorMessage())
                .build();
    }

    @Override
    public List<CompetitorVO> listProjectCompetitors(Long projectId, User user) {
        checkProjectOwnership(projectId, user);

        List<Competitor> list = competitorMapper.selectList(
                new LambdaQueryWrapper<Competitor>()
                        .eq(Competitor::getProjectId, projectId)
                        // 威胁等级排序: high → medium → low
                        .last("ORDER BY FIELD(threat_level,'high','medium','low'), created_at DESC"));

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public CompetitorVO getCompetitor(Long id, User user) {
        Competitor c = competitorMapper.selectById(id);
        if (c == null) throw new BusinessException(ResultCode.COMPETITOR_NOT_FOUND);
        checkProjectOwnership(c.getProjectId(), user);
        return toVO(c);
    }

    @Override
    public CompetitorVO updateCompetitor(Long id, UpdateCompetitorRequest req, User user) {
        Competitor c = competitorMapper.selectById(id);
        if (c == null) throw new BusinessException(ResultCode.COMPETITOR_NOT_FOUND);
        checkProjectOwnership(c.getProjectId(), user);

        if (req.getName() != null) c.setName(req.getName());
        if (req.getLogoUrl() != null) c.setLogoUrl(req.getLogoUrl());
        if (req.getWebsite() != null) c.setWebsite(req.getWebsite());
        if (req.getType() != null) c.setType(req.getType());
        if (req.getRegion() != null) c.setRegion(req.getRegion());
        if (req.getRating() != null) c.setRating(req.getRating());
        if (req.getMau() != null) c.setMau(req.getMau());
        if (req.getPricingModel() != null) c.setPricingModel(req.getPricingModel());
        if (req.getFundingStage() != null) c.setFundingStage(req.getFundingStage());
        if (req.getThreatLevel() != null) c.setThreatLevel(req.getThreatLevel());
        if (req.getFeatures() != null) c.setFeatures(writeJson(req.getFeatures()));
        if (req.getStrengths() != null) c.setStrengths(writeJson(req.getStrengths()));
        if (req.getWeaknesses() != null) c.setWeaknesses(writeJson(req.getWeaknesses()));
        if (req.getNeedsUserInput() != null) c.setNeedsUserInput(req.getNeedsUserInput() ? 1 : 0);

        competitorMapper.updateById(c);
        return toVO(c);
    }

    @Override
    public FeatureMatrixVO getFeatureMatrix(Long projectId, User user) {
        checkProjectOwnership(projectId, user);

        CompetitorReport report = reportMapper.selectOne(
                new LambdaQueryWrapper<CompetitorReport>().eq(CompetitorReport::getProjectId, projectId));

        if (report == null || report.getFeatureMatrix() == null) {
            return FeatureMatrixVO.builder().hasReport(false).build();
        }

        try {
            FeatureMatrixVO vo = objectMapper.readValue(report.getFeatureMatrix(), FeatureMatrixVO.class);
            vo.setHasReport(true);
            return vo;
        } catch (JsonProcessingException e) {
            log.warn("[CompetitorService] 矩阵 JSON 解析失败: projectId={}, error={}", projectId, e.getMessage());
            return FeatureMatrixVO.builder().hasReport(false).build();
        }
    }

    @Override
    public List<DifferentiationInsightVO> getInsights(Long projectId, User user) {
        checkProjectOwnership(projectId, user);

        CompetitorReport report = reportMapper.selectOne(
                new LambdaQueryWrapper<CompetitorReport>().eq(CompetitorReport::getProjectId, projectId));

        if (report == null || report.getDifferentiationInsights() == null) return List.of();

        return parseJsonList(report.getDifferentiationInsights(), DifferentiationInsightVO.class);
    }

    // ── 任务处理 ──────────────────────────────────────────────

    @Override
    @Transactional
    public void processTask(Long taskId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("[CompetitorService] 任务不存在: taskId={}", taskId);
            return;
        }
        if (!"pending".equals(task.getStatus())) {
            log.warn("[CompetitorService] 任务状态不是 pending, 跳过: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        task.setStatus("running");
        task.setProgress(10);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        try {
            Project project = projectMapper.selectById(task.getProjectId());
            if (project == null) throw new IllegalStateException("项目不存在: " + task.getProjectId());

            List<String> names = project.getCompetitors() != null ? project.getCompetitors() : List.of();
            if (names.isEmpty()) throw new IllegalStateException("项目未配置竞品名单");

            String description = project.getDescription() != null ? project.getDescription() : project.getName();

            task.setProgress(25);
            taskMapper.updateById(task);

            CompetitorAnalyzer.AnalysisOutput output = analyzer.analyze(
                    description, project.getIndustry(), project.getTargetMarket(), names);

            task.setProgress(80);
            taskMapper.updateById(task);

            persistResults(task.getProjectId(), output);

            task.setStatus("completed");
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            log.info("[CompetitorService] 任务完成: taskId={}, tokens={}", taskId, output.tokensUsed());

        } catch (Exception e) {
            log.error("[CompetitorService] 任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(truncate(e.getMessage(), 500));
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    // ── 私有工具 ──────────────────────────────────────────────

    /**
     * 持久化分析结果:
     *   1. 用 (projectId, 竞品名) 做 upsert: 已存在则更新, 不存在则新增
     *   2. 把每个竞品的支持度 scores[name][i] 与 features[i] 组装成 features JSON
     *   3. 上层 competitor_reports 表 upsert by project_id
     */
    private void persistResults(Long projectId, CompetitorAnalyzer.AnalysisOutput output) {
        CompetitorAnalysisResult r = output.result();
        List<String> features = r.getFeatureMatrix() != null ? r.getFeatureMatrix().getFeatures() : List.of();
        Map<String, List<String>> scores = r.getFeatureMatrix() != null && r.getFeatureMatrix().getScores() != null
                ? r.getFeatureMatrix().getScores() : Map.of();

        // 拉项目现有竞品做 name -> entity 索引
        Map<String, Competitor> existing = competitorMapper.selectList(
                new LambdaQueryWrapper<Competitor>().eq(Competitor::getProjectId, projectId))
                .stream().collect(Collectors.toMap(Competitor::getName, c -> c, (a, b) -> a));

        for (CompetitorAnalysisResult.CompetitorItem item : r.getCompetitors()) {
            if (item.getName() == null || item.getName().isBlank()) continue;

            // 把这个竞品的 features 行装成 {feature_name: score}
            Map<String, String> featMap = buildFeatureMap(features, scores.get(item.getName()));

            Competitor entity = existing.get(item.getName());
            if (entity == null) {
                entity = Competitor.builder()
                        .projectId(projectId)
                        .name(item.getName())
                        .createdAt(LocalDateTime.now())
                        .build();
            }
            entity.setType(defaultStr(item.getType(), "direct"));
            entity.setRegion(defaultStr(item.getRegion(), "未知"));
            entity.setRating(item.getRating() != null ? item.getRating() : BigDecimal.ZERO);
            entity.setPricingModel(item.getPricingModel());
            entity.setFundingStage(item.getFundingStage());
            entity.setThreatLevel(defaultStr(item.getThreatLevel(), "medium"));
            entity.setFeatures(writeJson(featMap));
            entity.setStrengths(writeJson(item.getStrengths()));
            entity.setWeaknesses(writeJson(item.getWeaknesses()));
            entity.setNeedsUserInput(Boolean.TRUE.equals(item.getNeedsUserInput()) ? 1 : 0);

            if (entity.getId() == null) competitorMapper.insert(entity);
            else competitorMapper.updateById(entity);
        }

        // 项目级报告 upsert
        CompetitorReport existingReport = reportMapper.selectOne(
                new LambdaQueryWrapper<CompetitorReport>().eq(CompetitorReport::getProjectId, projectId));

        Map<String, Object> matrixJson = new LinkedHashMap<>();
        matrixJson.put("features",      features);
        matrixJson.put("scores",        scores);
        matrixJson.put("opportunities", r.getFeatureMatrix() != null ? r.getFeatureMatrix().getOpportunities() : List.of());

        if (existingReport == null) {
            CompetitorReport report = CompetitorReport.builder()
                    .projectId(projectId)
                    .featureMatrix(writeJson(matrixJson))
                    .differentiationInsights(writeJson(r.getDifferentiationInsights()))
                    .aiModel(output.modelName())
                    .tokensUsed(output.tokensUsed())
                    .createdAt(LocalDateTime.now())
                    .build();
            reportMapper.insert(report);
        } else {
            existingReport.setFeatureMatrix(writeJson(matrixJson));
            existingReport.setDifferentiationInsights(writeJson(r.getDifferentiationInsights()));
            existingReport.setAiModel(output.modelName());
            existingReport.setTokensUsed(output.tokensUsed());
            reportMapper.updateById(existingReport);
        }
    }

    private Map<String, String> buildFeatureMap(List<String> features, List<String> scoreRow) {
        if (features == null || scoreRow == null) return Map.of();
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < features.size() && i < scoreRow.size(); i++) {
            map.put(features.get(i), scoreRow.get(i));
        }
        return map;
    }

    private void checkProjectOwnership(Long projectId, User user) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        if (!project.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);
    }

    private CompetitorVO toVO(Competitor c) {
        return CompetitorVO.builder()
                .id(c.getId())
                .projectId(c.getProjectId())
                .name(c.getName())
                .logoUrl(c.getLogoUrl())
                .website(c.getWebsite())
                .type(c.getType())
                .region(c.getRegion())
                .rating(c.getRating())
                .mau(c.getMau())
                .pricingModel(c.getPricingModel())
                .fundingStage(c.getFundingStage())
                .threatLevel(c.getThreatLevel())
                .features(parseJsonMap(c.getFeatures()))
                .strengths(parseJsonList(c.getStrengths(), String.class))
                .weaknesses(parseJsonList(c.getWeaknesses(), String.class))
                .needsUserInput(c.getNeedsUserInput() != null && c.getNeedsUserInput() == 1)
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : null)
                .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().format(FMT) : null)
                .build();
    }

    private String writeJson(Object obj) {
        if (obj == null) return null;
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return null; }
    }

    private <T> List<T> parseJsonList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) { return List.of(); }
    }

    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) { return Collections.emptyMap(); }
    }

    private String defaultStr(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
