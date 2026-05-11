package com.marketai.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marketai.backend.entity.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
