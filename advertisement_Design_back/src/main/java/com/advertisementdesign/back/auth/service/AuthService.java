package com.advertisementdesign.back.auth.service;

import com.advertisementdesign.back.auth.dto.AuthRequests;
import com.advertisementdesign.back.auth.security.JwtTokenService;
import com.advertisementdesign.back.auth.vo.AuthResponses;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.converter.UserConverter;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final IdentityService identityService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final EmailCodeMailService emailCodeMailService;
    private final EmailVerificationCodeStore emailCodeStore;
    private final CurrentActorProvider currentActorProvider;
    private static final Pattern CUSTOMER_EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@(qq\\.com|163\\.com|126\\.com|yeah\\.net)$", Pattern.CASE_INSENSITIVE);

    public AuthResponses.LoginResponse login(AuthRequests.LoginRequest request) {
        UserAccount user = findEnabledUser(request.email());
        if (user.role() == UserRole.CUSTOMER) {
            validateCustomerEmail(user.email());
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ApiException(401, "邮箱或密码错误");
        }
        return createLoginResponse(user);
    }

    public AuthResponses.SendEmailCodeResponse sendEmailCode(AuthRequests.SendEmailCodeRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        validateCustomerEmail(normalizedEmail);
        if (request.purpose() == AuthRequests.EmailCodePurpose.REGISTER) {
            findUserByEmail(normalizedEmail).ifPresent(user -> { throw new ApiException(400, "该邮箱已注册，请直接登录"); });
        } else {
            UserAccount user = findUserByEmail(normalizedEmail).orElseThrow(() -> new ApiException(400, "该邮箱尚未注册"));
            if (user.role() != UserRole.CUSTOMER || user.status() != UserStatus.ENABLED) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        Duration remaining = emailCodeStore.reserveSend(normalizedEmail, now, Duration.ofSeconds(60));
        if (!remaining.isZero()) {
            throw new ApiException(400, "请在 " + (remaining.toSeconds() + 1) + " 秒后重新发送");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        try {
            emailCodeMailService.sendCode(normalizedEmail, code);
        } catch (RuntimeException ex) {
            emailCodeStore.cancelSend(normalizedEmail, now);
            throw ex;
        }
        emailCodeStore.save(normalizedEmail, request.purpose(), code, now.plusSeconds(60));
        return new AuthResponses.SendEmailCodeResponse(60);
    }

    public AuthResponses.LoginResponse loginByEmailCode(AuthRequests.EmailCodeLoginRequest request) {
        UserAccount user = findEnabledUser(request.email());
        validateCustomerEmail(user.email());
        if (user.role() != UserRole.CUSTOMER) throw new ApiException(ApiErrorCode.FORBIDDEN);
        verifyEmailCode(user.email(), AuthRequests.EmailCodePurpose.LOGIN, request.code());
        return createLoginResponse(user);
    }

    @Transactional
    public boolean resetPassword(AuthRequests.ResetPasswordRequest request) {
        if (request.password().length() < 6) throw new ApiException(400, "密码长度不能少于 6 位");
        UserAccount user = findEnabledUser(request.email());
        validateCustomerEmail(user.email());
        if (user.role() != UserRole.CUSTOMER) throw new ApiException(ApiErrorCode.FORBIDDEN);
        verifyEmailCode(user.email(), AuthRequests.EmailCodePurpose.RESET_PASSWORD, request.code());
        identityService.updatePassword(user.id(), passwordEncoder.encode(request.password()));
        return true;
    }

    private AuthResponses.LoginResponse createLoginResponse(UserAccount user) {
        UserAccount updated = identityService.recordLogin(user.id());
        return new AuthResponses.LoginResponse(jwtTokenService.createToken(updated.id()), toUserVO(updated));
    }

    @Transactional
    public AuthResponses.UserVO register(AuthRequests.RegisterRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password()) || !StringUtils.hasText(request.nickname())) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        String email = normalizeEmail(request.email());
        validateCustomerEmail(email);
        if (request.password().length() < 6) throw new ApiException(400, "密码长度不能少于 6 位");
        findUserByEmail(email).ifPresent(user -> { throw new ApiException(400, "邮箱已存在"); });
        verifyEmailCode(email, AuthRequests.EmailCodePurpose.REGISTER, request.code());
        return toUserVO(identityService.createCustomer(email, passwordEncoder.encode(request.password()), request.nickname()));
    }

    public AuthResponses.UserVO me() {
        return toUserVO(currentUserAccount());
    }

    public AuthResponses.UserVO updateMe(com.advertisementdesign.back.identity.dto.UpdateUserRequest request) {
        Long userId = currentActorProvider.requireCurrentActor().actor().actorId();
        return toUserVO(identityService.updateProfile(userId, request.nickname(), request.avatarFileId(), request.phone()));
    }

    public boolean logout() { return true; }

    private UserAccount currentUserAccount() {
        return identityService.findAccountById(currentActorProvider.requireCurrentActor().actor().actorId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
    }

    private UserAccount findEnabledUser(String email) {
        UserAccount user = findUserByEmail(normalizeEmail(email)).orElseThrow(() -> new ApiException(401, "邮箱或密码错误"));
        if (user.status() != UserStatus.ENABLED) throw new ApiException(ApiErrorCode.FORBIDDEN);
        return user;
    }

    private void verifyEmailCode(String email, AuthRequests.EmailCodePurpose purpose, String code) {
        if (!emailCodeStore.consume(normalizeEmail(email), purpose, code, LocalDateTime.now())) {
            throw new ApiException(400, "验证码已过期、无效或错误，请重新获取");
        }
    }

    private Optional<UserAccount> findUserByEmail(String email) { return identityService.findByEmail(normalizeEmail(email)); }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(); }
    private void validateCustomerEmail(String email) {
        if (!CUSTOMER_EMAIL_PATTERN.matcher(normalizeEmail(email)).matches()) throw new ApiException(400, "仅支持 QQ 邮箱和网易邮箱（163、126、yeah.net）");
    }
    private AuthResponses.UserVO toUserVO(UserAccount user) {
        var identityUser = UserConverter.toVO(user);
        return new AuthResponses.UserVO(identityUser.id(), identityUser.email(), identityUser.nickname(), identityUser.role(), identityUser.avatar());
    }
}
