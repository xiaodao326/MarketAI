package com.marketai.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.project.AddKeywordsRequest;
import com.marketai.backend.dto.project.CreateProjectRequest;
import com.marketai.backend.dto.project.UpdateProjectRequest;
import com.marketai.backend.entity.Project;
import com.marketai.backend.mapper.ProjectMapper;
import com.marketai.backend.service.ProjectService;
import com.marketai.backend.vo.project.ProjectVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    /** 项目关键词上限 */
    private static final int MAX_KEYWORDS = 20;

    @Override
    public ProjectVO create(Long userId, CreateProjectRequest request) {
        Project project = Project.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .industry(request.getIndustry())
                .targetMarket(request.getTargetMarket())
                .keywords(request.getKeywords() != null ? request.getKeywords() : List.of())
                .competitors(request.getCompetitors() != null ? request.getCompetitors() : List.of())
                .status(0) // 默认草稿
                .build();
        save(project);
        return toVO(project);
    }

    @Override
    public Page<ProjectVO> listProjects(Long userId, int page, int size, Integer status, String keyword) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(status != null, Project::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Project::getName, keyword)
                        .or()
                        .like(Project::getDescription, keyword))
                .orderByDesc(Project::getCreatedAt);

        Page<Project> pageResult = page(new Page<>(page, size), wrapper);

        // 将 Page<Project> 转换为 Page<ProjectVO>
        Page<ProjectVO> voPage = new Page<>(page, size, pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public ProjectVO getDetail(Long userId, Long projectId) {
        Project project = getAndCheckOwnership(userId, projectId);
        return toVO(project);
    }

    @Override
    public ProjectVO update(Long userId, Long projectId, UpdateProjectRequest request) {
        Project project = getAndCheckOwnership(userId, projectId);

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getIndustry() != null) project.setIndustry(request.getIndustry());
        if (request.getTargetMarket() != null) project.setTargetMarket(request.getTargetMarket());
        if (request.getKeywords() != null) project.setKeywords(request.getKeywords());
        if (request.getCompetitors() != null) project.setCompetitors(request.getCompetitors());

        updateById(project);
        return toVO(getById(projectId));
    }

    @Override
    public void softDelete(Long userId, Long projectId) {
        Project project = getAndCheckOwnership(userId, projectId);
        project.setStatus(2); // 归档
        updateById(project);
    }

    @Override
    public ProjectVO addKeywords(Long userId, Long projectId, AddKeywordsRequest request) {
        Project project = getAndCheckOwnership(userId, projectId);

        List<String> existing = project.getKeywords() != null ? project.getKeywords() : new ArrayList<>();
        for (String kw : request.getKeywords()) {
            if (!existing.contains(kw)) {
                existing.add(kw);
            }
        }
        if (existing.size() > MAX_KEYWORDS) {
            throw new BusinessException(ResultCode.KEYWORD_LIMIT);
        }
        project.setKeywords(existing);
        updateById(project);
        return toVO(getById(projectId));
    }

    @Override
    public ProjectVO removeKeyword(Long userId, Long projectId, String keyword) {
        Project project = getAndCheckOwnership(userId, projectId);

        List<String> existing = project.getKeywords() != null ? new ArrayList<>(project.getKeywords()) : new ArrayList<>();
        existing.remove(keyword);
        project.setKeywords(existing);
        updateById(project);
        return toVO(getById(projectId));
    }

    /** 查询项目并校验所有权 */
    private Project getAndCheckOwnership(Long userId, Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PROJECT_FORBIDDEN);
        }
        return project;
    }

    private ProjectVO toVO(Project project) {
        return ProjectVO.builder()
                .id(project.getId())
                .userId(project.getUserId())
                .name(project.getName())
                .description(project.getDescription())
                .industry(project.getIndustry())
                .targetMarket(project.getTargetMarket())
                .keywords(project.getKeywords())
                .competitors(project.getCompetitors())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
