package com.marketai.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "更新个人信息请求")
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 20, message = "昵称长度2-20个字符")
    @Schema(description = "新昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像 URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
}