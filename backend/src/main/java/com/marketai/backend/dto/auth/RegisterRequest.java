package com.marketai.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "用户注册请求")
@Data
public class RegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "登录邮箱", example = "demo@marketai.com")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码长度至少8位")
    @Schema(description = "登录密码, 至少8位", example = "12345678")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称长度2-20个字符")
    @Schema(description = "用户昵称, 2-20个字符", example = "张三")
    private String nickname;
}