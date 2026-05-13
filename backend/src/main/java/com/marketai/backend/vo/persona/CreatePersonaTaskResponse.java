package com.marketai.backend.vo.persona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonaTaskResponse {
    private Long   taskId;
    private String status;            // "pending"
    private int    estimatedSeconds;  // 约 45 秒
}
