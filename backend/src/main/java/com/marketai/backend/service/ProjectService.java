package com.marketai.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.marketai.backend.dto.project.AddKeywordsRequest;
import com.marketai.backend.dto.project.CreateProjectRequest;
import com.marketai.backend.dto.project.UpdateProjectRequest;
import com.marketai.backend.entity.Project;
import com.marketai.backend.vo.project.ProjectVO;

public interface ProjectService extends IService<Project> {

    ProjectVO create(Long userId, CreateProjectRequest request);

    Page<ProjectVO> listProjects(Long userId, int page, int size, Integer status, String keyword);

    ProjectVO getDetail(Long userId, Long projectId);

    ProjectVO update(Long userId, Long projectId, UpdateProjectRequest request);

    void softDelete(Long userId, Long projectId);

    ProjectVO addKeywords(Long userId, Long projectId, AddKeywordsRequest request);

    ProjectVO removeKeyword(Long userId, Long projectId, String keyword);
}
