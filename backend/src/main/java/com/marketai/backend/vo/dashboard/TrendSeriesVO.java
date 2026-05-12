package com.marketai.backend.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendSeriesVO {
    private String name;
    private List<Long> data;
    private int yAxisIndex;
}
