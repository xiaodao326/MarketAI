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
public class TrendDataVO {
    private List<String> dates;
    private List<TrendSeriesVO> series;
}
