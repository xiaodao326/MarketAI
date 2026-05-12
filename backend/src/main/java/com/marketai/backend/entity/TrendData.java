package com.marketai.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("trend_data")
public class TrendData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String keyword;

    /** 数据源: baidu_index / weibo */
    private String source;

    /** 指标类型: search_volume / social_mentions */
    private String metricType;

    private BigDecimal value;

    private LocalDate dataDate;

    private LocalDateTime createdAt;
}
