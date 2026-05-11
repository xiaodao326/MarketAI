package com.marketai.backend.service.impl;

import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.Constants;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.auth.LoginRequest;
import com.marketai.backend.dto.auth.RegisterRequest;
import com.marketai.backend.dto.auth.UpdateProfileRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.security.JwtUtils;
import com.marketai.backend.service.AuthService;
import com.marketai.backend.service.UserService;
import com.marketai.backend.vo.auth.LoginResponse;
import com.marketai.backend.vo.auth.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Object> register(RegisterRequest request) {
        User user = userService.register(request);
        String token = generateAndCacheToken(user);
        return Map.of("user_id", user.getId(), "token", token);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.AUTH_FAILED);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.AUTH_FAILED);
        }
        String token = generateAndCacheToken(user);
        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .token(token)
                .build();
    }

    @Override
    public void logout(Long userId) {
        redisTemplate.delete(Constants.REDIS_TOKEN_PREFIX + userId);
    }

    @Override
    public UserInfo getCurrentUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return toUserInfo(user);
    }

    @Override
    public UserInfo updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request);
        return toUserInfo(user);
    }

    private String generateAndCacheToken(User user) {
        String token = jwtUtils.generateToken(user.getId(), user.getEmail());
        String redisKey = Constants.REDIS_TOKEN_PREFIX + user.getId();
        Duration ttl = Duration.ofMillis(jwtUtils.getExpirationMillis());
        redisTemplate.opsForValue().set(redisKey, token, ttl);
        return token;
    }

    private UserInfo toUserInfo(User user) {
        return UserInfo.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}