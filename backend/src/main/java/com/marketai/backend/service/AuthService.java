package com.marketai.backend.service;

import com.marketai.backend.dto.auth.LoginRequest;
import com.marketai.backend.dto.auth.RegisterRequest;
import com.marketai.backend.dto.auth.UpdateProfileRequest;
import com.marketai.backend.vo.auth.LoginResponse;
import com.marketai.backend.vo.auth.UserInfo;

import java.util.Map;

public interface AuthService {

    Map<String, Object> register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(Long userId);

    UserInfo getCurrentUser(Long userId);

    UserInfo updateProfile(Long userId, UpdateProfileRequest request);
}