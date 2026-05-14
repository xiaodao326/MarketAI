package com.marketai.backend.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "追加关键词请求")
@Data
public class AddKeywordsRequest {

    @NotEmpty(message = "关键词不能为空")
    @Size(max = 20, message = "单次最多添加20个关键词")
    @Schema(description = "要添加的关键词列表", example = "[\"对话式AI\",\"智能工单\"]")
    private List<String> keywords;
}
