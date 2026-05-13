package com.marketai.backend.controller;

import com.marketai.backend.ai.persona.dto.PersonaGenerateRequest;
import com.marketai.backend.common.Result;
import com.marketai.backend.dto.persona.CreatePersonaRequest;
import com.marketai.backend.dto.persona.UpdatePersonaRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.service.PersonaService;
import com.marketai.backend.vo.persona.CreatePersonaTaskResponse;
import com.marketai.backend.vo.persona.PersonaTaskStatusVO;
import com.marketai.backend.vo.persona.PersonaVO;
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
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
@Tag(name = "用户画像", description = "AI 生成与手动管理目标用户画像")
public class PersonaController {

    private final PersonaService personaService;

    @PostMapping("/generate")
    @Operation(summary = "触发 AI 生成画像", description = "异步任务,立即返回 taskId")
    public Result<CreatePersonaTaskResponse> generate(
            @Valid @RequestBody PersonaGenerateRequest request,
            @AuthenticationPrincipal User user) {
        return Result.success(personaService.createTask(request, user));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询画像生成任务状态")
    public Result<PersonaTaskStatusVO> getTaskStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        return Result.success(personaService.getTaskStatus(taskId, user));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目的全部画像", description = "核心用户置顶,然后按 market_share 降序")
    public Result<List<PersonaVO>> listProjectPersonas(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return Result.success(personaService.listProjectPersonas(projectId, user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个画像")
    public Result<PersonaVO> getPersona(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return Result.success(personaService.getPersona(id, user));
    }

    @PostMapping
    @Operation(summary = "手动创建画像")
    public Result<PersonaVO> create(
            @Valid @RequestBody CreatePersonaRequest request,
            @AuthenticationPrincipal User user) {
        return Result.success(personaService.createPersona(request, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新画像", description = "部分字段更新,只传需要修改的字段")
    public Result<PersonaVO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePersonaRequest request,
            @AuthenticationPrincipal User user) {
        return Result.success(personaService.updatePersona(id, request, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除画像")
    public Result<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        personaService.deletePersona(id, user);
        return Result.success();
    }
}
