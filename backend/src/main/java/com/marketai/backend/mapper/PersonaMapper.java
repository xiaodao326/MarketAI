package com.marketai.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marketai.backend.entity.Persona;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PersonaMapper extends BaseMapper<Persona> {
}
