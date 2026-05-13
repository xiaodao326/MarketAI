package com.marketai.backend.vo.persona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaTaskStatusVO {
    private Long    taskId;
    private String  status;        // pending / running / completed / failed
    private Integer progress;      // 0-100
    private Long    projectId;     // completed 后前端用此 ID 拉新画像列表
    private String  errorMessage;
}
