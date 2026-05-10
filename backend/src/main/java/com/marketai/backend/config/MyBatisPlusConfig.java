package com.marketai.backend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.marketai.backend.mapper")
public class MyBatisPlusConfig {
}
