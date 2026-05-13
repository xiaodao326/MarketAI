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
 * 用户画像实体
 * JSON 列 (goals/painPoints/decisionParams) 与 InsightReport 同样以原始字符串存储,
 * 由 Service 层使用 ObjectMapper 解析。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("personas")
public class Persona {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;
    private String role;
    private String ageRange;

    /** 在目标市场中的占比 (百分比, 0-100) */
    private BigDecimal marketShare;

    /** 0=普通 / 1=核心用户 */
    private Integer isPrimary;

    private String goals;          // JSON: ["目标1", "目标2"]
    private String painPoints;     // JSON: [{title, description}]
    private String quote;
    private String decisionParams; // JSON: {paymentWillingness, decisionCycle, budgetRange, techCapability}

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
