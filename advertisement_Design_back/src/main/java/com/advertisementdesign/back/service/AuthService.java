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
import com.advertisementdesign.back.mapper.UserMapper;
import com.advertisementdesign.back.security.JwtTokenService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final ApiAssembler assembler;
    private final EmailCodeMailService emailCodeMailService;
    private final Map<String, EmailCode> emailCodes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> emailCodeLastSentAt = new ConcurrentHashMap<>();
    private static final Pattern CUSTOMER_EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@(qq\\.com|163\\.com|126\\.com|yeah\\.net)$",
            Pattern.CASE_INSENSITIVE);

    private record EmailCode(String code, LocalDateTime expiresAt) {
    }

    public AuthModels.LoginResponse login(AuthModels.LoginRequest request) {
        UserEntity user = findEnabledUser(request.email());
        if (user.getRole() == UserRole.CUSTOMER) {
            validateCustomerEmail(user.getEmail());
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(401, "邮箱或密码错误");
        }
        return createLoginResponse(user);
    }

    public AuthModels.SendEmailCodeResponse sendEmailCode(AuthModels.SendEmailCodeRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        validateCustomerEmail(normalizedEmail);
        if (request.purpose() == AuthModels.EmailCodePurpose.REGISTER) {
            findUserByEmail(normalizedEmail).ifPresent(user -> {
                throw new ApiException(400, "该邮箱已注册，请直接登录");
            });
        } else {
            UserEntity user = findUserByEmail(normalizedEmail)
                    .orElseThrow(() -> new ApiException(400, "该邮箱尚未注册"));
            if (user.getRole() != UserRole.CUSTOMER || user.getStatus() != UserStatus.ENABLED) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        synchronized (emailCodeLastSentAt) {
            LocalDateTime lastSentAt = emailCodeLastSentAt.get(normalizedEmail);
            if (lastSentAt != null && lastSentAt.plusSeconds(60).isAfter(now)) {
                long remaining = java.time.Duration.between(now, lastSentAt.plusSeconds(60)).toSeconds() + 1;
                throw new ApiException(400, "请在 " + remaining + " 秒后重新发送");
            }
            emailCodeLastSentAt.put(normalizedEmail, now);
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        try {
            emailCodeMailService.sendCode(normalizedEmail, code);
        } catch (RuntimeException ex) {
            synchronized (emailCodeLastSentAt) {
                if (now.equals(emailCodeLastSentAt.get(normalizedEmail))) {
                    emailCodeLastSentAt.remove(normalizedEmail);
                }
            }
            throw ex;
        }
        emailCodes.put(codeKey(normalizedEmail, request.purpose()), new EmailCode(code, now.plusSeconds(60)));
        return new AuthModels.SendEmailCodeResponse(60);
    }

    public AuthModels.LoginResponse loginByEmailCode(AuthModels.EmailCodeLoginRequest request) {
        UserEntity user = findEnabledUser(request.email());
        validateCustomerEmail(user.getEmail());
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        verifyEmailCode(user.getEmail(), AuthModels.EmailCodePurpose.LOGIN, request.code());
        return createLoginResponse(user);
    }

    @Transactional
    public boolean resetPassword(AuthModels.ResetPasswordRequest request) {
        if (request.password().length() < 6) {
            throw new ApiException(400, "密码长度不能少于 6 位");
        }
        UserEntity user = findEnabledUser(request.email());
        validateCustomerEmail(user.getEmail());
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        verifyEmailCode(user.getEmail(), AuthModels.EmailCodePurpose.RESET_PASSWORD, request.code());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUpdatedAt(LocalDateTime.now());
        saveUser(user);
        return true;
    }

    private AuthModels.LoginResponse createLoginResponse(UserEntity user) {
        user.setLastLoginAt(LocalDateTime.now());
        saveUser(user);
        CurrentUser currentUser = CurrentUser.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
        return assembler.toLoginResponse(jwtTokenService.createToken(currentUser), user);
    }

    @Transactional
    public AuthModels.UserVO register(AuthModels.RegisterRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password()) || !StringUtils.hasText(request.nickname())) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        String email = normalizeEmail(request.email());
        validateCustomerEmail(email);
        if (request.password().length() < 6) {
            throw new ApiException(400, "密码长度不能少于 6 位");
        }
        findUserByEmail(email).ifPresent(user -> {
            throw new ApiException(400, "邮箱已存在");
        });
        verifyEmailCode(email, AuthModels.EmailCodePurpose.REGISTER, request.code());
        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(UserRole.CUSTOMER)
                .avatar(null)
                .status(UserStatus.ENABLED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return assembler.toUserVO(saveUser(user));
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
        return assembler.toUserVO(saveUser(user));
    }

    public boolean logout() {
        return true;
    }

    public UserEntity currentUserEntity() {
        CurrentUser currentUser = AuthContext.currentUser();
        return findUserById(currentUser.getId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
    }

    private UserEntity findEnabledUser(String email) {
        UserEntity user = findUserByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiException(401, "邮箱或密码错误"));
        if (user.getStatus() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return user;
    }

    private void verifyEmailCode(String email, AuthModels.EmailCodePurpose purpose, String code) {
        String key = codeKey(normalizeEmail(email), purpose);
        EmailCode stored = emailCodes.get(key);
        if (stored == null || stored.expiresAt().isBefore(LocalDateTime.now())) {
            emailCodes.remove(key);
            throw new ApiException(400, "验证码已过期或无效，请重新获取");
        }
        if (!stored.code().equals(code)) {
            throw new ApiException(400, "验证码错误");
        }
        emailCodes.remove(key);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String codeKey(String email, AuthModels.EmailCodePurpose purpose) {
        return purpose.name() + ":" + email;
    }

    private void validateCustomerEmail(String email) {
        if (!CUSTOMER_EMAIL_PATTERN.matcher(normalizeEmail(email)).matches()) {
            throw new ApiException(400, "仅支持 QQ 邮箱和网易邮箱（163、126、yeah.net）");
        }
    }

    private Optional<UserEntity> findUserByEmail(String email) {
        return Optional.ofNullable(userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getEmail, normalizeEmail(email))
                        .last("LIMIT 1")
        ));
    }

    private Optional<UserEntity> findUserById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id));
    }

    private UserEntity saveUser(UserEntity user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user;
    }
}
