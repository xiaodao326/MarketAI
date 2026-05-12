package com.marketai.backend.vo.insight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 历史报告列表条目（不含完整 JSON 内容）*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightReportSummaryVO {
    private Long    id;
    private String  title;
    private Integer marketFitScore;
    private String  aiModel;
    private Integer tokensUsed;
    private String  createdAt;
    private String  completedAt;
}
