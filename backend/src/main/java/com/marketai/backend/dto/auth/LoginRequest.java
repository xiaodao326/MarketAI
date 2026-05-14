package com.marketai.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户登录请求")
@Data
public class LoginRequest {

    @NotBlank(message = "邮箱不能为空")
    @Schema(description = "登录邮箱", example = "demo@marketai.com")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "12345678")
    private String password;
}