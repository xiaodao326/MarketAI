package com.marketai.backend.dto.project;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProjectRequest {

    @Size(max = 50, message = "项目名称最长50个字符")
    private String name;

    private String description;

    private String industry;

    private String targetMarket;

    @Size(max = 20, message = "关键词最多20个")
    private List<String> keywords;

    private List<String> competitors;
}
