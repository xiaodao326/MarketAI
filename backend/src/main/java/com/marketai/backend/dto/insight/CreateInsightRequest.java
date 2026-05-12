package com.marketai.backend.dto.insight;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateInsightRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "产品描述不能为空")
    @Size(max = 2000, message = "产品描述不超过 2000 字")
    private String productDescription;

    /** lite / standard / full, 默认 standard */
    private String depth = "standard";
}
