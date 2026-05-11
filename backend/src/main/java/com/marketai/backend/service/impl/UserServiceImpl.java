package com.marketai.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marketai.backend.common.BusinessException;
import com.marketai.backend.common.ResultCode;
import com.marketai.backend.dto.auth.RegisterRequest;
import com.marketai.backend.dto.auth.UpdateProfileRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.mapper.UserMapper;
import com.marketai.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {
        if (existsByEmail(request.getEmail())) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(1);
        save(user);
        return user;
    }

    @Override
    public User findByEmail(String email) {
        return getOne(new QueryWrapper<User>().eq("email", email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return baseMapper.selectCount(new QueryWrapper<User>().eq("email", email)) > 0;
    }

    @Override
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        updateById(user);
        return getById(userId);
    }
}