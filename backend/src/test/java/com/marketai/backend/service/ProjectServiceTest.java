package com.marketai.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.dto.project.AddKeywordsRequest;
import com.marketai.backend.dto.project.CreateProjectRequest;
import com.marketai.backend.dto.project.UpdateProjectRequest;
import com.marketai.backend.entity.Project;
import com.marketai.backend.mapper.ProjectMapper;
import com.marketai.backend.service.impl.ProjectServiceImpl;
import com.marketai.backend.vo.project.ProjectVO;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService 单元测试")
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PROJECT_ID = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(projectService, "baseMapper", projectMapper);
    }

    private Project buildProject(Long id, Long userId, String name, Integer status) {
        return Project.builder()
                .id(id).userId(userId).name(name)
                .description("描述").industry("企业服务").targetMarket("中国")
                .keywords(new ArrayList<>(List.of("AI客服")))
                .competitors(new ArrayList<>(List.of("竞品A")))
                .status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("创建项目")
    class Create {

        @Test
        @DisplayName("正常创建 — 状态为草稿(0), 返回完整 ProjectVO")
        void shouldCreateProject() {
            when(projectMapper.insert(any(Project.class))).thenReturn(1);

            CreateProjectRequest req = new CreateProjectRequest();
            req.setName("AI客服市场分析");
            req.setDescription("分析AI客服赛道的市场机会");
            req.setIndustry("企业服务");
            req.setTargetMarket("中国");
            req.setKeywords(List.of("智能客服", "AI客服"));
            req.setCompetitors(List.of("Zendesk"));

            ProjectVO vo = projectService.create(USER_ID, req);

            assertThat(vo.getUserId()).isEqualTo(USER_ID);
            assertThat(vo.getName()).isEqualTo("AI客服市场分析");
            assertThat(vo.getStatus()).isEqualTo(0);
            assertThat(vo.getKeywords()).containsExactly("智能客服", "AI客服");

            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("keywords 为 null — 存储空列表")
        void shouldHandleNullKeywords() {
            when(projectMapper.insert(any(Project.class))).thenReturn(1);

            CreateProjectRequest req = new CreateProjectRequest();
            req.setName("测试");
            req.setIndustry("电商零售");
            req.setTargetMarket("东南亚");

            ProjectVO vo = projectService.create(USER_ID, req);
            assertThat(vo.getKeywords()).isEmpty();
            assertThat(vo.getCompetitors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("查询项目列表")
    class ListProjects {

        @Test
        @DisplayName("分页查询 — 仅返回当前用户的项目")
        void shouldReturnPagedProjects() {
            Project p1 = buildProject(1L, USER_ID, "项目A", 0);
            Project p2 = buildProject(2L, USER_ID, "项目B", 1);
            Page<Project> mockPage = new Page<>(1, 12);
            mockPage.setRecords(List.of(p1, p2));
            mockPage.setTotal(2);

            when(projectMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            Page<ProjectVO> result = projectService.listProjects(USER_ID, 1, 12, null, null);

            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getRecords().get(0).getUserId()).isEqualTo(USER_ID);
            assertThat(result.getRecords().get(1).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("按状态筛选")
        void shouldFilterByStatus() {
            Page<Project> mockPage = new Page<>(1, 12);
            mockPage.setRecords(List.of(buildProject(1L, USER_ID, "活跃项目", 1)));
            mockPage.setTotal(1);

            when(projectMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            Page<ProjectVO> result = projectService.listProjects(USER_ID, 1, 12, 1, null);
            assertThat(result.getTotal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询项目详情")
    class GetDetail {

        @Test
        @DisplayName("正常查询 — 用户是所有者")
        void shouldReturnProjectDetail() {
            Project project = buildProject(PROJECT_ID, USER_ID, "我的项目", 1);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

            ProjectVO vo = projectService.getDetail(USER_ID, PROJECT_ID);
            assertThat(vo.getId()).isEqualTo(PROJECT_ID);
            assertThat(vo.getName()).isEqualTo("我的项目");
        }

        @Test
        @DisplayName("项目不存在 — 抛出 PROJECT_NOT_FOUND(2001)")
        void shouldThrowWhenNotFound() {
            when(projectMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> projectService.getDetail(USER_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(2001);
        }

        @Test
        @DisplayName("非所有者访问 — 抛出 PROJECT_FORBIDDEN(2002)")
        void shouldThrowWhenNotOwner() {
            Project project = buildProject(PROJECT_ID, USER_ID, "别人的项目", 1);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

            assertThatThrownBy(() -> projectService.getDetail(OTHER_USER_ID, PROJECT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(2002);
        }
    }

    @Nested
    @DisplayName("更新项目")
    class Update {

        @Test
        @DisplayName("部分更新 — 只更新传入的字段")
        void shouldPartialUpdate() {
            Project project = buildProject(PROJECT_ID, USER_ID, "旧名称", 1);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(projectMapper.updateById(any(Project.class))).thenReturn(1);

            UpdateProjectRequest req = new UpdateProjectRequest();
            req.setName("新名称");
            req.setDescription("新描述");

            ProjectVO vo = projectService.update(USER_ID, PROJECT_ID, req);
            assertThat(vo.getName()).isEqualTo("新名称");
            assertThat(vo.getDescription()).isEqualTo("新描述");
            assertThat(vo.getIndustry()).isEqualTo("企业服务"); // 未变更
        }

        @Test
        @DisplayName("非所有者更新 — 抛出 PROJECT_FORBIDDEN(2002)")
        void shouldThrowWhenNotOwner() {
            Project project = buildProject(PROJECT_ID, USER_ID, "项目", 1);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

            UpdateProjectRequest req = new UpdateProjectRequest();
            req.setName("恶意修改");

            assertThatThrownBy(() -> projectService.update(OTHER_USER_ID, PROJECT_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(2002);

            verify(projectMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("软删除(归档)")
    class SoftDelete {

        @Test
        @DisplayName("归档 — status 设为 2")
        void shouldArchiveProject() {
            Project project = buildProject(PROJECT_ID, USER_ID, "待归档", 1);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(projectMapper.updateById(any(Project.class))).thenReturn(1);

            projectService.softDelete(USER_ID, PROJECT_ID);

            assertThat(project.getStatus()).isEqualTo(2);
            verify(projectMapper).updateById(project);
        }

        @Test
        @DisplayName("非所有者归档 — 抛出 PROJECT_FORBIDDEN(2002)")
        void shouldThrowWhenNotOwner() {
            Project project = buildProject(PROJECT_ID, USER_ID, "项目", 1);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

            assertThatThrownBy(() -> projectService.softDelete(OTHER_USER_ID, PROJECT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(2002);
        }
    }

    @Nested
    @DisplayName("关键词管理")
    class Keywords {

        @Test
        @DisplayName("追加关键词 — 去重后追加")
        void shouldAddKeywords() {
            Project project = buildProject(PROJECT_ID, USER_ID, "项目", 1);
            project.setKeywords(new ArrayList<>(List.of("AI客服", "智能客服")));
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(projectMapper.updateById(any(Project.class))).thenReturn(1);

            AddKeywordsRequest req = new AddKeywordsRequest();
            req.setKeywords(List.of("AI客服", "大模型", "NLP")); // "AI客服" 已存在, 应去重

            ProjectVO vo = projectService.addKeywords(USER_ID, PROJECT_ID, req);
            assertThat(vo.getKeywords()).containsExactlyInAnyOrder("AI客服", "智能客服", "大模型", "NLP");
        }

        @Test
        @DisplayName("超限 — keyword 总数超过 20 抛出 KEYWORD_LIMIT(2003)")
        void shouldThrowWhenExceedsLimit() {
            Project project = buildProject(PROJECT_ID, USER_ID, "项目", 1);
            List<String> almostFull = new ArrayList<>();
            for (int i = 0; i < 18; i++) almostFull.add("keyword_" + i);
            project.setKeywords(almostFull);
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);

            AddKeywordsRequest req = new AddKeywordsRequest();
            req.setKeywords(List.of("new1", "new2", "new3")); // 18 + 3 = 21 > 20

            assertThatThrownBy(() -> projectService.addKeywords(USER_ID, PROJECT_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(2003);

            verify(projectMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("移除关键词")
        void shouldRemoveKeyword() {
            Project project = buildProject(PROJECT_ID, USER_ID, "项目", 1);
            project.setKeywords(new ArrayList<>(List.of("AI客服", "智能客服", "大模型")));
            when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
            when(projectMapper.updateById(any(Project.class))).thenReturn(1);

            ProjectVO vo = projectService.removeKeyword(USER_ID, PROJECT_ID, "智能客服");
            assertThat(vo.getKeywords()).containsExactly("AI客服", "大模型");
        }
    }
}
