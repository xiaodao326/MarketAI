package com.marketai.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.Result;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.project.AddKeywordsRequest;
import com.marketai.backend.dto.project.CreateProjectRequest;
import com.marketai.backend.dto.project.UpdateProjectRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.service.ProjectService;
import com.marketai.backend.vo.project.ProjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "项目", description = "分析项目的创建、查询、更新、删除")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "创建项目")
    public Result<ProjectVO> create(@AuthenticationPrincipal User user,
                                    @Valid @RequestBody CreateProjectRequest request) {
        checkAuth(user);
        return Result.success(projectService.create(user.getId(), request));
    }

    @GetMapping
    @Operation(summary = "项目列表", description = "分页查询当前用户的项目,支持状态筛选和关键词搜索")
    public Result<Map<String, Object>> list(@AuthenticationPrincipal User user,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "12") int size,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String keyword) {
        checkAuth(user);
        Page<ProjectVO> result = projectService.listProjects(user.getId(), page, size, status, keyword);
        return Result.success(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "pages", result.getPages()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "项目详情")
    public Result<ProjectVO> detail(@AuthenticationPrincipal User user,
                                     @PathVariable Long id) {
        checkAuth(user);
        return Result.success(projectService.getDetail(user.getId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目")
    public Result<ProjectVO> update(@AuthenticationPrincipal User user,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdateProjectRequest request) {
        checkAuth(user);
        return Result.success(projectService.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "归档项目", description = "软删除,将项目状态设为归档(2)")
    public Result<Void> delete(@AuthenticationPrincipal User user,
                                @PathVariable Long id) {
        checkAuth(user);
        projectService.softDelete(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/keywords")
    @Operation(summary = "追加关键词")
    public Result<ProjectVO> addKeywords(@AuthenticationPrincipal User user,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AddKeywordsRequest request) {
        checkAuth(user);
        return Result.success(projectService.addKeywords(user.getId(), id, request));
    }

    @DeleteMapping("/{id}/keywords/{keyword}")
    @Operation(summary = "移除关键词")
    public Result<ProjectVO> removeKeyword(@AuthenticationPrincipal User user,
                                            @PathVariable Long id,
                                            @PathVariable String keyword) {
        checkAuth(user);
        return Result.success(projectService.removeKeyword(user.getId(), id, keyword));
    }

    private void checkAuth(User user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }
}
