package com.marketai.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单个竞品实体 — 一行一个竞品
 * JSON 列 (features/reviews/strengths/weaknesses) 以原始字符串存储, 由 Service 层解析
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("competitors")
public class Competitor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;
    private String logoUrl;
    private String website;

    /** direct / indirect / potential */
    private String type;

    private String region;
    private BigDecimal rating;
    private Integer mau;
    private String pricingModel;
    private String fundingStage;

    /** high / medium / low */
    private String threatLevel;

    private String features;    // JSON: {"feature_name":"yes"|"partial"|"no"}
    private String reviews;     // JSON: 数组
    private String strengths;   // JSON: 数组
    private String weaknesses;  // JSON: 数组

    /** AI 标记的"需要用户补充"标志: 0=已知 / 1=不熟悉 */
    private Integer needsUserInput;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
