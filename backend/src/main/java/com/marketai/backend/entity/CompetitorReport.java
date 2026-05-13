package com.marketai.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目级的竞品对比报告 — 每个项目一行 (upsert by project_id)
 * 存放跨竞品的聚合数据 (功能矩阵特征列表 + 差异化建议),
 * 而单竞品的支持度仍存在 competitors.features 中
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("competitor_reports")
public class CompetitorReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** JSON: {"features":["功能1",...], "opportunities":["功能1: 空白",...]} */
    private String featureMatrix;

    /** JSON: 差异化建议数组 */
    private String differentiationInsights;

    private String aiModel;
    private Integer tokensUsed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
