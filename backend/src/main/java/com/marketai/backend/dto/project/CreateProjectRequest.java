package com.marketai.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 50, message = "项目名称最长50个字符")
    private String name;

    private String description;

    @NotBlank(message = "行业标签不能为空")
    private String industry;

    @NotBlank(message = "目标市场不能为空")
    private String targetMarket;

    @Size(min = 1, max = 20, message = "关键词至少1个,最多20个")
    private List<String> keywords;

    private List<String> competitors;
}
