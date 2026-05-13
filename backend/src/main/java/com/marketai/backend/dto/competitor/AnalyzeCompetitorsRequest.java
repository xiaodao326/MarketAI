package com.marketai.backend.dto.competitor;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnalyzeCompetitorsRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;
}
