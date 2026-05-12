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
 * AI 需求洞察报告实体
 * JSON 列 (dimensions/painPoints/opportunities/risks/actions) 以原始字符串存储,
 * 由 Service 层使用 ObjectMapper 解析成结构化 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("insight_reports")
public class InsightReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String title;

    /** completed / failed (报告只在成功时创建) */
    private String status;

    private Integer marketFitScore;

    private String dimensions;    // JSON
    private String painPoints;    // JSON
    private String opportunities; // JSON
    private String risks;         // JSON
    private String actions;       // JSON

    private String aiModel;
    private Integer tokensUsed;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
