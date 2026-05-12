package com.marketai.backend.vo.insight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusVO {
    private Long    taskId;
    private String  status;       // pending / running / completed / failed
    private Integer progress;     // 0-100
    private Long    resultId;     // completed 时指向 insight_reports.id
    private String  errorMessage;
}
