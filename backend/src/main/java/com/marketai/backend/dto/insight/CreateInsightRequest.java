package com.marketai.backend.dto.insight;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "创建 AI 需求洞察请求")
@Data
public class CreateInsightRequest {

    @NotNull(message = "项目 ID 不能为空")
    @Schema(description = "所属项目 ID", example = "1")
    private Long projectId;

    @NotBlank(message = "产品描述不能为空")
    @Size(max = 2000, message = "产品描述不超过 2000 字")
    @Schema(description = "产品/服务描述", example = "一款面向中小企业的智能客服SaaS产品")
    private String productDescription;

    @Schema(description = "分析深度: lite/standard/full", example = "standard")
    private String depth = "standard";
}
