package com.marketai.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marketai.backend.entity.AnalysisTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalysisTaskMapper extends BaseMapper<AnalysisTask> {

    /** 统计用户当前 pending 或 running 状态的指定类型任务数 (用于限流) */
    @Select("SELECT COUNT(*) FROM analysis_tasks WHERE user_id = #{userId} AND task_type = #{taskType} AND status IN ('pending', 'running')")
    int countActiveTasks(@Param("userId") Long userId, @Param("taskType") String taskType);
}
