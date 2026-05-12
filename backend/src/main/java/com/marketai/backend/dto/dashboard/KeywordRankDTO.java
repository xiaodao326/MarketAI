package com.marketai.backend.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class KeywordRankDTO {
    private String keyword;
    private BigDecimal currentVolume;
    private BigDecimal prevVolume;
}
