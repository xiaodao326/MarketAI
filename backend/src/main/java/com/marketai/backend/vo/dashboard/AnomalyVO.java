package com.marketai.backend.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyVO {
    private String id;
    private String severity; // critical / warning / opportunity
    private String title;
    private String description;
    private String source;
    private LocalDate occurredAt;
}
