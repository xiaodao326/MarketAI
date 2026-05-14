package com.marketai.backend.dto.competitor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "触发竞品分析请求")
@Data
public class AnalyzeCompetitorsRequest {

    @NotNull(message = "项目 ID 不能为空")
    @Schema(description = "所属项目 ID", example = "1")
    private Long projectId;
}
