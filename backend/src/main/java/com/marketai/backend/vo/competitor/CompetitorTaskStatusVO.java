package com.marketai.backend.vo.competitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorTaskStatusVO {
    private Long    taskId;
    private String  status;
    private Integer progress;
    private Long    projectId;
    private String  errorMessage;
}
