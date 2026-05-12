package com.marketai.backend.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordRankVO {
    private int rank;
    private String keyword;
    private long volume;
    private double deltaPercent;
}
