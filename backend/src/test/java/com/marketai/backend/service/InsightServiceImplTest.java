package com.marketai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketai.backend.ai.insight.InsightAnalyzer;
import com.marketai.backend.ai.insight.InsightTaskQueue;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.insight.CreateInsightRequest;
import com.marketai.backend.entity.AnalysisTask;
import com.marketai.backend.entity.Project;
import com.marketai.backend.entity.User;
import com.marketai.backend.mapper.AnalysisTaskMapper;
import com.marketai.backend.mapper.InsightReportMapper;
import com.marketai.backend.mapper.ProjectMapper;
import com.marketai.backend.service.impl.InsightServiceImpl;
import com.marketai.backend.vo.insight.CreateInsightResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsightServiceImpl 单元测试")
class InsightServiceImplTest {

    @Mock private AnalysisTaskMapper  taskMapper;
    @Mock private InsightReportMapper reportMapper;
    @Mock private ProjectMapper       projectMapper;
    @Mock private InsightTaskQueue    taskQueue;
    @Mock private InsightAnalyzer     analyzer;

    @InjectMocks
    private InsightServiceImpl insightService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final Long USER_ID    = 1L;
    private static final Long PROJECT_ID = 10L;

    private User    user;
    private Project project;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(insightService, "objectMapper", objectMapper);

        user = new User();
        user.setId(USER_ID);

        project = new Project();
        project.setId(PROJECT_ID);
        project.setUserId(USER_ID);
        project.setName("测试项目");
        project.setIndustry("企业服务");
        project.setTargetMarket("中国");
        project.setKeywords(List.of("AI客服"));
    }

    // ── 创建任务 ──────────────────────────────────────────────

    @Nested
    @DisplayName("createTask()")
    class CreateTaskTest {

        @Test
        @DisplayName("正常创建 — 返回 taskId 和 pending 状态")
        void shouldCreateTaskAndReturnPending() {
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(taskMapper.countActiveTasks(USER_ID, "insight")).thenReturn(0);
            when(taskMapper.insert(any())).thenReturn(1);
            doNothing().when(taskQueue).enqueue(any());

            CreateInsightRequest req = new CreateInsightRequest();
            req.setProjectId(PROJECT_ID);
            req.setProductDescription("一款 AI 客服产品");
            req.setDepth("standard");

            CreateInsightResponse resp = insightService.createTask(req, user);

            assertThat(resp.getStatus()).isEqualTo("pending");
            assertThat(resp.getEstimatedSeconds()).isEqualTo(60);

            // 验证任务入队
            verify(taskQueue).enqueue(any());

            // 验证持久化的任务字段
            ArgumentCaptor<AnalysisTask> captor = ArgumentCaptor.forClass(AnalysisTask.class);
            verify(taskMapper).insert(captor.capture());
            AnalysisTask saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(saved.getStatus()).isEqualTo("pending");
            assertThat(saved.getInputParams()).contains("AI 客服产品");
        }

        @Test
        @DisplayName("估算时间 — lite=30s, standard=60s, full=120s")
        void shouldReturnCorrectEstimatedSeconds() {
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(taskMapper.countActiveTasks(USER_ID, "insight")).thenReturn(0);
            when(taskMapper.insert(any())).thenReturn(1);

            for (var testCase : new Object[][]{{"lite", 30}, {"standard", 60}, {"full", 120}}) {
                CreateInsightRequest req = new CreateInsightRequest();
                req.setProjectId(PROJECT_ID);
                req.setProductDescription("测试产品");
                req.setDepth((String) testCase[0]);

                int estimated = insightService.createTask(req, user).getEstimatedSeconds();
                assertThat(estimated).isEqualTo(testCase[1]);
            }
        }

        @Test
        @DisplayName("项目不存在 — 抛出 PROJECT_NOT_FOUND")
        void shouldThrowWhenProjectNotFound() {
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(null);

            CreateInsightRequest req = new CreateInsightRequest();
            req.setProjectId(PROJECT_ID);
            req.setProductDescription("测试");

            assertThatThrownBy(() -> insightService.createTask(req, user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ResultCode.PROJECT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("其他用户的项目 — 抛出 PROJECT_FORBIDDEN")
        void shouldThrowWhenProjectBelongsToOtherUser() {
            Project otherProject = new Project();
            otherProject.setId(PROJECT_ID);
            otherProject.setUserId(999L); // 其他用户
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(otherProject);

            CreateInsightRequest req = new CreateInsightRequest();
            req.setProjectId(PROJECT_ID);
            req.setProductDescription("测试");

            assertThatThrownBy(() -> insightService.createTask(req, user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ResultCode.PROJECT_FORBIDDEN.getMessage());
        }
    }

    // ── 限流 ──────────────────────────────────────────────────

    @Nested
    @DisplayName("限流校验")
    class RateLimitTest {

        @Test
        @DisplayName("并发任务达 3 个 — 抛出 INSIGHT_TASK_LIMIT (429)")
        void shouldThrowWhenActiveTasksReachLimit() {
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(taskMapper.countActiveTasks(USER_ID, "insight")).thenReturn(3);

            CreateInsightRequest req = new CreateInsightRequest();
            req.setProjectId(PROJECT_ID);
            req.setProductDescription("测试");

            assertThatThrownBy(() -> insightService.createTask(req, user))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(ResultCode.INSIGHT_TASK_LIMIT.getCode()));
        }

        @Test
        @DisplayName("并发任务 2 个 — 可以继续创建")
        void shouldAllowCreationWhenUnderLimit() {
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(taskMapper.countActiveTasks(USER_ID, "insight")).thenReturn(2);
            when(taskMapper.insert(any())).thenReturn(1);

            CreateInsightRequest req = new CreateInsightRequest();
            req.setProjectId(PROJECT_ID);
            req.setProductDescription("测试");

            assertThatNoException().isThrownBy(() -> insightService.createTask(req, user));
        }
    }

    // ── 任务状态流转 ───────────────────────────────────────────

    @Nested
    @DisplayName("processTask() 状态流转")
    class TaskStatusFlowTest {

        @Test
        @DisplayName("任务不存在 — 直接返回，不抛异常")
        void shouldReturnSilentlyWhenTaskNotFound() {
            when(taskMapper.selectById(42L)).thenReturn(null);
            assertThatNoException().isThrownBy(() -> insightService.processTask(42L));
            verify(analyzer, never()).analyze(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("任务状态已非 pending — 跳过处理")
        void shouldSkipNonPendingTask() {
            AnalysisTask task = AnalysisTask.builder()
                    .id(1L).userId(USER_ID).projectId(PROJECT_ID)
                    .taskType("insight").status("running").build();
            when(taskMapper.selectById(1L)).thenReturn(task);

            insightService.processTask(1L);

            verify(analyzer, never()).analyze(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("AI 调用失败 — 任务标记为 failed，errorMessage 被记录")
        void shouldMarkTaskFailedOnAnalyzerException() {
            AnalysisTask task = AnalysisTask.builder()
                    .id(1L).userId(USER_ID).projectId(PROJECT_ID)
                    .taskType("insight").status("pending")
                    .inputParams("{\"productDescription\":\"产品\",\"depth\":\"standard\"}")
                    .build();
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(analyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("AI 服务不可用"));

            insightService.processTask(1L);

            ArgumentCaptor<AnalysisTask> captor = ArgumentCaptor.forClass(AnalysisTask.class);
            verify(taskMapper, atLeastOnce()).updateById(captor.capture());

            AnalysisTask lastUpdate = captor.getAllValues().getLast();
            assertThat(lastUpdate.getStatus()).isEqualTo("failed");
            assertThat(lastUpdate.getErrorMessage()).contains("AI 服务不可用");
            assertThat(lastUpdate.getCompletedAt()).isNotNull();
        }
    }
}
