package com.marketai.backend.dto.auth;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 20, message = "昵称长度2-20个字符")
    private String nickname;

    private String avatarUrl;
}