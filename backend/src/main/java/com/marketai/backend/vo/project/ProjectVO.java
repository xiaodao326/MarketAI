package com.marketai.backend.vo.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVO {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String industry;
    private String targetMarket;
    private List<String> keywords;
    private List<String> competitors;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
