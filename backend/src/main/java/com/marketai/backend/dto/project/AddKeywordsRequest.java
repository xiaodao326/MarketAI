package com.marketai.backend.dto.project;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AddKeywordsRequest {

    @NotEmpty(message = "关键词不能为空")
    @Size(max = 20, message = "单次最多添加20个关键词")
    private List<String> keywords;
}
