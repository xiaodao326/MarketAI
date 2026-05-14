package com.marketai.backend.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "更新项目请求")
@Data
public class UpdateProjectRequest {

    @Size(max = 50, message = "项目名称最长50个字符")
    @Schema(description = "项目名称", example = "智能客服市场分析 v2")
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "行业标签", example = "SaaS")
    private String industry;

    @Schema(description = "目标市场区域", example = "中国")
    private String targetMarket;

    @Size(max = 20, message = "关键词最多20个")
    @Schema(description = "监控关键词列表")
    private List<String> keywords;

    @Schema(description = "竞品名单")
    private List<String> competitors;
}
