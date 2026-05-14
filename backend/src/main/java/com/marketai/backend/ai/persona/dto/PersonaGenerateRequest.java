package com.marketai.backend.ai.persona.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "AI 生成用户画像请求")
@Data
public class PersonaGenerateRequest {

    @NotNull(message = "项目 ID 不能为空")
    @Schema(description = "所属项目 ID", example = "1")
    private Long projectId;

    @Min(value = 3, message = "生成数量至少 3 个")
    @Max(value = 5, message = "生成数量至多 5 个")
    @Schema(description = "生成数量, 3-5", example = "4")
    private Integer count = 4;

    @Size(max = 500, message = "额外上下文不超过 500 字")
    @Schema(description = "可选的额外上下文 (用户补充信息)")
    private String context;
}
