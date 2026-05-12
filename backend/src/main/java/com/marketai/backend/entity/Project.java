package com.marketai.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.marketai.backend.handler.JsonListTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "projects", autoResultMap = true)
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String description;

    private String industry;

    /** 数据库列名 target_market, MyBatis-Plus 自动映射为 targetMarket */
    private String targetMarket;

    @TableField(typeHandler = JsonListTypeHandler.class)
    private List<String> keywords;

    @TableField(typeHandler = JsonListTypeHandler.class)
    private List<String> competitors;

    /** 项目状态: 0=草稿, 1=活跃, 2=归档 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
