package com.marketai.backend.vo.competitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureMatrixVO {
    /** 功能列表 (顺序与 scores 中数组的下标对齐) */
    private List<String> features;
    /** 每个竞品的支持度数组: name -> ["yes"|"partial"|"no"] */
    private Map<String, List<String>> scores;
    /** 机会标记 */
    private List<String> opportunities;
    /** 是否已有报告 */
    private boolean hasReport;
}
