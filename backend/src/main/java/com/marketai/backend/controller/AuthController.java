package com.marketai.backend.controller;

import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.Result;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.auth.LoginRequest;
import com.marketai.backend.dto.auth.RegisterRequest;
import com.marketai.backend.dto.auth.UpdateProfileRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.service.AuthService;
import com.marketai.backend.vo.auth.LoginResponse;
import com.marketai.backend.vo.auth.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户注册、登录、个人信息管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册成功后自动登录返回 token")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = authService.register(request);
        return Result.success(result);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse result = authService.login(request);
        return Result.success(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "使当前 token 失效")
    public Result<Void> logout(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        authService.logout(user.getId());
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfo> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return Result.success(authService.getCurrentUser(user.getId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人信息")
    public Result<UserInfo> updateProfile(@AuthenticationPrincipal User user,
                                          @Valid @RequestBody UpdateProfileRequest request) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return Result.success(authService.updateProfile(user.getId(), request));
    }
}