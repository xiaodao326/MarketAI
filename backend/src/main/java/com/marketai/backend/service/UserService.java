package com.marketai.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.marketai.backend.dto.auth.RegisterRequest;
import com.marketai.backend.dto.auth.UpdateProfileRequest;
import com.marketai.backend.entity.User;

public interface UserService extends IService<User> {

    User register(RegisterRequest request);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User updateProfile(Long userId, UpdateProfileRequest request);
}