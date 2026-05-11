package com.marketai.backend.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private Long userId;
    private String email;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime createdAt;
}