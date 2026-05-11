package com.marketai.backend.service;

import com.marketai.backend.common.BusinessException;
import com.marketai.backend.dto.auth.RegisterRequest;
import com.marketai.backend.dto.auth.UpdateProfileRequest;
import com.marketai.backend.entity.User;
import com.marketai.backend.mapper.UserMapper;
import com.marketai.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 通过 @Autowired 注入, 纯 Mockito 环境需手动赋值
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Nested
    @DisplayName("用户注册")
    class Register {

        @Test
        @DisplayName("正常注册 — 密码 BCrypt 加密, 返回用户信息")
        void shouldRegisterSuccessfully() {
            when(userMapper.selectCount(any())).thenReturn(0L);
            when(userMapper.insert(any(User.class))).thenReturn(1);
            when(passwordEncoder.encode("password123")).thenReturn("$2b$10$encrypted_hash");

            RegisterRequest req = new RegisterRequest();
            req.setEmail("test@marketai.com");
            req.setPassword("password123");
            req.setNickname("测试用户");

            User user = userService.register(req);

            assertThat(user.getEmail()).isEqualTo("test@marketai.com");
            assertThat(user.getPasswordHash()).isEqualTo("$2b$10$encrypted_hash");
            assertThat(user.getNickname()).isEqualTo("测试用户");
            assertThat(user.getStatus()).isEqualTo(1);
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("重复邮箱 — 抛出 EMAIL_EXISTS(1001)")
        void shouldRejectDuplicateEmail() {
            when(userMapper.selectCount(any())).thenReturn(1L);

            RegisterRequest req = new RegisterRequest();
            req.setEmail("dup@marketai.com");
            req.setPassword("password123");
            req.setNickname("重复用户");

            assertThatThrownBy(() -> userService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(1001);

            verify(userMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("邮箱存在性")
    class EmailQuery {

        @Test
        @DisplayName("不存在返回 false")
        void shouldReturnFalse() {
            when(userMapper.selectCount(any())).thenReturn(0L);
            assertThat(userService.existsByEmail("nobody@test.com")).isFalse();
        }

        @Test
        @DisplayName("已存在返回 true")
        void shouldReturnTrue() {
            when(userMapper.selectCount(any())).thenReturn(1L);
            assertThat(userService.existsByEmail("demo@marketai.com")).isTrue();
        }
    }

    @Nested
    @DisplayName("个人信息")
    class UpdateProfile {

        @Test
        @DisplayName("更新昵称成功")
        void shouldUpdateNickname() {
            User existing = User.builder()
                    .id(1L).email("p@t.com").nickname("旧昵称")
                    .passwordHash("hash").status(1).build();
            when(userMapper.selectById(1L)).thenReturn(existing);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setNickname("新昵称");

            User updated = userService.updateProfile(1L, req);
            assertThat(updated.getNickname()).isEqualTo("新昵称");
        }

        @Test
        @DisplayName("用户不存在 — 抛出异常(404)")
        void shouldThrowWhenNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setNickname("x");

            assertThatThrownBy(() -> userService.updateProfile(999L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(404);
        }
    }
}