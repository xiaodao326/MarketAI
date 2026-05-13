package com.marketai.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketai.backend.ai.persona.PersonaGenerator;
import com.marketai.backend.ai.persona.PersonaTaskQueue;
import com.marketai.backend.ai.persona.dto.PersonaGenerateRequest;
import com.marketai.backend.ai.persona.dto.PersonaResult;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.persona.CreatePersonaRequest;
import com.marketai.backend.dto.persona.UpdatePersonaRequest;
import com.marketai.backend.entity.AnalysisTask;
import com.marketai.backend.entity.Persona;
import com.marketai.backend.entity.Project;
import com.marketai.backend.entity.User;
import com.marketai.backend.mapper.AnalysisTaskMapper;
import com.marketai.backend.mapper.PersonaMapper;
import com.marketai.backend.mapper.ProjectMapper;
import com.marketai.backend.service.PersonaService;
import com.marketai.backend.vo.persona.CreatePersonaTaskResponse;
import com.marketai.backend.vo.persona.PersonaTaskStatusVO;
import com.marketai.backend.vo.persona.PersonaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonaServiceImpl implements PersonaService {

    private static final int    MAX_ACTIVE_TASKS = 3;
    private static final String TASK_TYPE        = "persona";
    private static final DateTimeFormatter FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalysisTaskMapper taskMapper;
    private final PersonaMapper      personaMapper;
    private final ProjectMapper      projectMapper;
    private final PersonaTaskQueue   taskQueue;
    private final PersonaGenerator   generator;
    private final ObjectMapper       objectMapper;

    // ── 对外接口 ──────────────────────────────────────────────

    @Override
    public CreatePersonaTaskResponse createTask(PersonaGenerateRequest req, User user) {
        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        if (!project.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        int count = req.getCount() != null ? req.getCount() : 4;
        if (count < 3 || count > 5) throw new BusinessException(ResultCode.PERSONA_COUNT_INVALID);

        int active = taskMapper.countActiveTasks(user.getId(), TASK_TYPE);
        if (active >= MAX_ACTIVE_TASKS) throw new BusinessException(ResultCode.PERSONA_TASK_LIMIT);

        String inputParams;
        try {
            inputParams = objectMapper.writeValueAsString(
                    Map.of("count", count, "context", req.getContext() != null ? req.getContext() : ""));
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
        log.info("[PersonaService] 任务已创建: taskId={}, userId={}, projectId={}, count={}",
                task.getId(), user.getId(), req.getProjectId(), count);

        return CreatePersonaTaskResponse.builder()
                .taskId(task.getId())
                .status("pending")
                .estimatedSeconds(45)
                .build();
    }

    @Override
    public PersonaTaskStatusVO getTaskStatus(Long taskId, User user) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ResultCode.PERSONA_TASK_NOT_FOUND);
        if (!task.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);

        return PersonaTaskStatusVO.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .progress(task.getProgress())
                .projectId(task.getProjectId())
                .errorMessage(task.getErrorMessage())
                .build();
    }

    @Override
    public List<PersonaVO> listProjectPersonas(Long projectId, User user) {
        checkProjectOwnership(projectId, user);

        List<Persona> list = personaMapper.selectList(
                new LambdaQueryWrapper<Persona>()
                        .eq(Persona::getProjectId, projectId)
                        // 排序: 核心用户置顶, 其次按 market_share 降序
                        .orderByDesc(Persona::getIsPrimary)
                        .orderByDesc(Persona::getMarketShare)
                        .orderByDesc(Persona::getCreatedAt));

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PersonaVO getPersona(Long id, User user) {
        Persona p = personaMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.PERSONA_NOT_FOUND);
        checkProjectOwnership(p.getProjectId(), user);
        return toVO(p);
    }

    @Override
    public PersonaVO createPersona(CreatePersonaRequest req, User user) {
        checkProjectOwnership(req.getProjectId(), user);

        Persona p = Persona.builder()
                .projectId(req.getProjectId())
                .name(req.getName())
                .role(req.getRole())
                .ageRange(req.getAgeRange())
                .marketShare(req.getMarketShare() != null ? req.getMarketShare() : BigDecimal.ZERO)
                .isPrimary(Boolean.TRUE.equals(req.getIsPrimary()) ? 1 : 0)
                .goals(writeJson(req.getGoals()))
                .painPoints(writeJson(req.getPainPoints()))
                .quote(req.getQuote())
                .decisionParams(writeJson(req.getDecisionParams()))
                .createdAt(LocalDateTime.now())
                .build();
        personaMapper.insert(p);

        // 若新建为核心用户, 取消该项目其他核心用户标记 (保证唯一核心)
        if (p.getIsPrimary() == 1) demoteOtherPrimaries(p.getProjectId(), p.getId());

        return toVO(p);
    }

    @Override
    public PersonaVO updatePersona(Long id, UpdatePersonaRequest req, User user) {
        Persona p = personaMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.PERSONA_NOT_FOUND);
        checkProjectOwnership(p.getProjectId(), user);

        if (req.getName() != null) p.setName(req.getName());
        if (req.getRole() != null) p.setRole(req.getRole());
        if (req.getAgeRange() != null) p.setAgeRange(req.getAgeRange());
        if (req.getMarketShare() != null) p.setMarketShare(req.getMarketShare());
        if (req.getIsPrimary() != null) p.setIsPrimary(req.getIsPrimary() ? 1 : 0);
        if (req.getGoals() != null) p.setGoals(writeJson(req.getGoals()));
        if (req.getPainPoints() != null) p.setPainPoints(writeJson(req.getPainPoints()));
        if (req.getQuote() != null) p.setQuote(req.getQuote());
        if (req.getDecisionParams() != null) p.setDecisionParams(writeJson(req.getDecisionParams()));

        personaMapper.updateById(p);

        if (Boolean.TRUE.equals(req.getIsPrimary())) demoteOtherPrimaries(p.getProjectId(), p.getId());

        return toVO(p);
    }

    @Override
    public void deletePersona(Long id, User user) {
        Persona p = personaMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.PERSONA_NOT_FOUND);
        checkProjectOwnership(p.getProjectId(), user);
        personaMapper.deleteById(id);
    }

    // ── 任务处理 ──────────────────────────────────────────────

    @Override
    public void processTask(Long taskId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("[PersonaService] 任务不存在: taskId={}", taskId);
            return;
        }
        if (!"pending".equals(task.getStatus())) {
            log.warn("[PersonaService] 任务状态不是 pending, 跳过: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        task.setStatus("running");
        task.setProgress(10);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        try {
            Project project = projectMapper.selectById(task.getProjectId());
            if (project == null) throw new IllegalStateException("项目不存在: " + task.getProjectId());

            Map<String, Object> params = objectMapper.readValue(task.getInputParams(), new TypeReference<>() {});
            int count          = ((Number) params.getOrDefault("count", 4)).intValue();
            String extraCtx    = (String) params.getOrDefault("context", "");
            String description = project.getDescription() != null ? project.getDescription() : project.getName();

            task.setProgress(25);
            taskMapper.updateById(task);

            PersonaGenerator.GenerationOutput output = generator.generate(
                    task.getProjectId(), description,
                    project.getIndustry(), project.getTargetMarket(),
                    project.getKeywords() != null ? project.getKeywords() : List.of(),
                    count, extraCtx);

            task.setProgress(85);
            taskMapper.updateById(task);

            persistPersonas(task.getProjectId(), output.personas());

            task.setStatus("completed");
            task.setProgress(100);
            // resultId 不指向单一画像, 前端拿 projectId 重新拉列表
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            log.info("[PersonaService] 任务完成: taskId={}, personas={}, tokens={}",
                    taskId, output.personas().size(), output.tokensUsed());

        } catch (Exception e) {
            log.error("[PersonaService] 任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(truncate(e.getMessage(), 500));
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    // ── 私有工具 ──────────────────────────────────────────────

    /**
     * 将 AI 生成的画像入库, 保证恰好一个 is_primary=1:
     *   1. 优先使用 AI 给出的 isPrimary=true 标记
     *   2. 若 AI 没标 / 标了多个 / 没标任何一个, 按 (market_share * payment_score) 算最高分
     *   3. 先清除项目原有的 is_primary 标记, 再写入新数据
     */
    private void persistPersonas(Long projectId, List<PersonaResult> personas) {
        int primaryIdx = pickPrimaryIndex(personas);

        // 清除项目原有的 is_primary 标记 — 新一轮生成视为完全替换的核心标记权
        personaMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Persona>()
                        .eq(Persona::getProjectId, projectId)
                        .eq(Persona::getIsPrimary, 1)
                        .set(Persona::getIsPrimary, 0));

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < personas.size(); i++) {
            PersonaResult r = personas.get(i);
            Persona p = Persona.builder()
                    .projectId(projectId)
                    .name(r.getName())
                    .role(r.getRole())
                    .ageRange(r.getAgeRange() != null ? r.getAgeRange() : "未知")
                    .marketShare(BigDecimal.valueOf(r.getMarketShare() != null ? r.getMarketShare() : 0))
                    .isPrimary(i == primaryIdx ? 1 : 0)
                    .goals(writeJson(r.getGoals()))
                    .painPoints(writeJson(r.getPainPoints()))
                    .quote(r.getQuote())
                    .decisionParams(writeJson(r.getDecisionParams()))
                    .createdAt(now)
                    .build();
            personaMapper.insert(p);
        }
    }

    private int pickPrimaryIndex(List<PersonaResult> personas) {
        // 优先 AI 标记
        long aiMarked = personas.stream().filter(p -> Boolean.TRUE.equals(p.getIsPrimary())).count();
        if (aiMarked == 1) {
            for (int i = 0; i < personas.size(); i++) {
                if (Boolean.TRUE.equals(personas.get(i).getIsPrimary())) return i;
            }
        }
        // 否则按 (market_share * payment_score) 评分, payment: high=3 / medium=2 / low=1
        return java.util.stream.IntStream.range(0, personas.size()).boxed()
                .max(Comparator.comparingDouble(i -> scoreFor(personas.get(i))))
                .orElse(0);
    }

    private double scoreFor(PersonaResult p) {
        int share = p.getMarketShare() != null ? p.getMarketShare() : 0;
        int pay   = 2;
        if (p.getDecisionParams() != null) {
            String pw = p.getDecisionParams().getPaymentWillingness();
            if ("high".equals(pw)) pay = 3;
            else if ("low".equals(pw)) pay = 1;
        }
        return share * pay;
    }

    /** 把项目内除指定画像外的所有 is_primary 设为 0 */
    private void demoteOtherPrimaries(Long projectId, Long keepId) {
        personaMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Persona>()
                        .eq(Persona::getProjectId, projectId)
                        .ne(Persona::getId, keepId)
                        .eq(Persona::getIsPrimary, 1)
                        .set(Persona::getIsPrimary, 0));
    }

    private void checkProjectOwnership(Long projectId, User user) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        if (!project.getUserId().equals(user.getId())) throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);
    }

    private PersonaVO toVO(Persona p) {
        return PersonaVO.builder()
                .id(p.getId())
                .projectId(p.getProjectId())
                .name(p.getName())
                .role(p.getRole())
                .ageRange(p.getAgeRange())
                .marketShare(p.getMarketShare())
                .isPrimary(p.getIsPrimary() != null && p.getIsPrimary() == 1)
                .goals(parseJsonList(p.getGoals(), String.class))
                .painPoints(parseJsonList(p.getPainPoints(), PersonaResult.PainPoint.class))
                .quote(p.getQuote())
                .decisionParams(parseJson(p.getDecisionParams(), PersonaResult.DecisionParams.class))
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().format(FMT) : null)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().format(FMT) : null)
                .build();
    }

    private String writeJson(Object obj) {
        if (obj == null) return null;
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return null; }
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
