package com.marketai.backend.vo.insight;

import com.marketai.backend.ai.insight.dto.InsightResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightReportVO {

    private Long   id;
    private Long   projectId;
    private String title;

    private Integer                       marketFitScore;
    private InsightResult.Dimensions      dimensions;
    private List<InsightResult.PainPoint> painPoints;
    private List<InsightResult.Opportunity> opportunities;
    private List<InsightResult.Risk>      risks;
    private List<InsightResult.ActionItem> actions;

    private String  aiModel;
    private Integer tokensUsed;
    private String  createdAt;
    private String  completedAt;
}
