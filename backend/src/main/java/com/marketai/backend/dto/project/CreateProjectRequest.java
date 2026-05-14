package com.marketai.backend.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "创建分析项目请求")
@Data
public class CreateProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 50, message = "项目名称最长50个字符")
    @Schema(description = "项目名称", example = "智能客服市场分析")
    private String name;

    @Schema(description = "项目描述", example = "分析国内智能客服SaaS市场规模与机会")
    private String description;

    @NotBlank(message = "行业标签不能为空")
    @Schema(description = "行业标签", example = "SaaS")
    private String industry;

    @NotBlank(message = "目标市场不能为空")
    @Schema(description = "目标市场区域", example = "中国")
    private String targetMarket;

    @Size(min = 1, max = 20, message = "关键词至少1个,最多20个")
    @Schema(description = "监控关键词列表", example = "[\"智能客服\",\"AI客服\"]")
    private List<String> keywords;

    @Schema(description = "竞品名单", example = "[\"Zendesk\",\"Intercom\"]")
    private List<String> competitors;
}
