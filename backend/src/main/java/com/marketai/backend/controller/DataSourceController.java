package com.marketai.backend.controller;

import com.marketai.backend.ai.datasource.CollectorService;
import com.marketai.backend.ai.datasource.DataSourceConnector;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.Result;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/datasource")
@RequiredArgsConstructor
@Tag(name = "数据采集", description = "手动触发采集任务、查看数据源健康状态")
public class DataSourceController {

    private final CollectorService collectorService;
    private final List<DataSourceConnector> connectors;

    @PostMapping("/collect/{projectId}")
    @Operation(summary = "手动触发采集", description = "异步采集指定项目所有关键词的最近 7 天趋势数据")
    public Result<String> triggerCollect(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        if (user == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        log.info("[API] 手动触发采集: projectId={}, userId={}", projectId, user.getId());
        CompletableFuture.runAsync(() -> collectorService.collectForProject(projectId));
        return Result.success("采集任务已触发, 数据将在后台写入");
    }

    @GetMapping("/status")
    @Operation(summary = "数据源健康状态", description = "逐一检查各数据源连通性")
    public Result<List<Map<String, Object>>> status(@AuthenticationPrincipal User user) {
        if (user == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        List<Map<String, Object>> statuses = connectors.stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("source", c.getSourceName());
                    m.put("healthy", c.healthCheck());
                    return m;
                })
                .toList();
        return Result.success(statuses);
    }
}
