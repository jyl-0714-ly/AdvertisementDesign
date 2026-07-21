package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.auth.AuthModels;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.mapper.UserMapper;
import com.advertisementdesign.back.security.JwtTokenService;
import com.advertisementdesign.back.store.DemoDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private EmailCodeMailService emailCodeMailService;

    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userMapper,
                passwordEncoder,
                jwtTokenService,
                new ApiAssembler(new DemoDataStore()),
                emailCodeMailService
        );
    }

    @Test
    void registerPersistsUserAndLoginReadsPersistedUser() {
        String email = "new-customer@163.com";
        String password = "123456";
        AtomicReference<UserEntity> persistedUser = new AtomicReference<>();

        when(userMapper.selectOne(any())).thenAnswer(invocation -> persistedUser.get());
        when(userMapper.insert(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(100L);
            persistedUser.set(user);
            return 1;
        });
        when(userMapper.updateById(any())).thenReturn(1);
        when(jwtTokenService.createToken(any())).thenReturn("test-token");

        authService.sendEmailCode(new AuthModels.SendEmailCodeRequest(
                email,
                AuthModels.EmailCodePurpose.REGISTER
        ));
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailCodeMailService).sendCode(eq(email), codeCaptor.capture());

        AuthModels.UserVO registered = authService.register(new AuthModels.RegisterRequest(
                email,
                codeCaptor.getValue(),
                password,
                "新客户"
        ));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(userCaptor.capture());
        UserEntity insertedUser = userCaptor.getValue();
        assertEquals(100L, registered.id());
        assertEquals(email, insertedUser.getEmail());
        assertEquals(UserRole.CUSTOMER, insertedUser.getRole());
        assertTrue(passwordEncoder.matches(password, insertedUser.getPasswordHash()));

        AuthModels.LoginResponse login = authService.login(new AuthModels.LoginRequest(email, password));

        assertEquals("test-token", login.token());
        assertNotNull(login.user());
        assertEquals(100L, login.user().id());
        verify(userMapper).updateById(insertedUser);
    }
}
