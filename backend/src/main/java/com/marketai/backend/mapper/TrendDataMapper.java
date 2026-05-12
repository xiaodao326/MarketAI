package com.marketai.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marketai.backend.entity.TrendData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrendDataMapper extends BaseMapper<TrendData> {

    /**
     * 批量插入趋势数据, 重复数据自动忽略 (依赖 uk_trend_data 唯一索引)
     */
    void insertIgnoreBatch(@Param("list") List<TrendData> list);
}
