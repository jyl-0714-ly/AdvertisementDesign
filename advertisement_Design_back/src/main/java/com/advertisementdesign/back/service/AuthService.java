package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.auth.AuthModels;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.AuthContext;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.domain.enums.UserStatus;
import com.advertisementdesign.back.security.JwtTokenService;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final DemoDataStore store;
    private final JwtTokenService jwtTokenService;
    private final ApiAssembler assembler;

    public AuthModels.LoginResponse login(AuthModels.LoginRequest request) {
        UserEntity user = store.findUserByEmail(request.email())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
        if (user.getStatus() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (!store.passwordEncoder().matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED);
        }
        user.setLastLoginAt(LocalDateTime.now());
        store.saveUser(user);
        CurrentUser currentUser = CurrentUser.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
        return assembler.toLoginResponse(jwtTokenService.createToken(currentUser), user);
    }

    public AuthModels.UserVO register(AuthModels.RegisterRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password()) || !StringUtils.hasText(request.nickname())) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        store.findUserByEmail(request.email()).ifPresent(user -> {
            throw new ApiException(400, "邮箱已存在");
        });
        UserEntity user = UserEntity.builder()
                .email(request.email())
                .passwordHash(store.passwordEncoder().encode(request.password()))
                .nickname(request.nickname())
                .role(UserRole.CUSTOMER)
                .avatar(null)
                .status(UserStatus.ENABLED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return assembler.toUserVO(store.saveUser(user));
    }

    public AuthModels.UserVO me() {
        return assembler.toUserVO(currentUserEntity());
    }

    public AuthModels.UserVO updateMe(AuthModels.UpdateUserRequest request) {
        UserEntity user = currentUserEntity();
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return assembler.toUserVO(store.saveUser(user));
    }

    public boolean logout() {
        return true;
    }

    public UserEntity currentUserEntity() {
        CurrentUser currentUser = AuthContext.currentUser();
        return store.findUserById(currentUser.getId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
    }
}
