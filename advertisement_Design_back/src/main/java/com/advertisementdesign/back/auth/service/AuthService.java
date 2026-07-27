package com.advertisementdesign.back.auth.service;

import com.advertisementdesign.back.auth.dto.AuthRequests;
import com.advertisementdesign.back.auth.security.JwtTokenService;
import com.advertisementdesign.back.auth.vo.AuthResponses;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.AuthContext;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.identity.converter.UserConverter;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserAccount;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final IdentityService identityService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final EmailCodeMailService emailCodeMailService;
    private final Map<String, EmailCode> emailCodes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> emailCodeLastSentAt = new ConcurrentHashMap<>();
    private static final Pattern CUSTOMER_EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@(qq\\.com|163\\.com|126\\.com|yeah\\.net)$", Pattern.CASE_INSENSITIVE);

    private record EmailCode(String code, LocalDateTime expiresAt) {}

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
                if (now.equals(emailCodeLastSentAt.get(normalizedEmail))) emailCodeLastSentAt.remove(normalizedEmail);
            }
            throw ex;
        }
        emailCodes.put(codeKey(normalizedEmail, request.purpose()), new EmailCode(code, now.plusSeconds(60)));
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
        CurrentUser currentUser = CurrentUser.builder().id(updated.id()).email(updated.email())
                .nickname(updated.nickname()).role(updated.role()).build();
        return new AuthResponses.LoginResponse(jwtTokenService.createToken(currentUser), toUserVO(updated));
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
        CurrentUser currentUser = AuthContext.currentUser();
        return toUserVO(identityService.updateProfile(currentUser.getId(), request.nickname(), request.avatar(), request.phone()));
    }

    public boolean logout() { return true; }

    private UserAccount currentUserAccount() {
        return identityService.findByEmail(AuthContext.currentUser().getEmail())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
    }

    private UserAccount findEnabledUser(String email) {
        UserAccount user = findUserByEmail(normalizeEmail(email)).orElseThrow(() -> new ApiException(401, "邮箱或密码错误"));
        if (user.status() != UserStatus.ENABLED) throw new ApiException(ApiErrorCode.FORBIDDEN);
        return user;
    }

    private void verifyEmailCode(String email, AuthRequests.EmailCodePurpose purpose, String code) {
        String key = codeKey(normalizeEmail(email), purpose);
        EmailCode stored = emailCodes.get(key);
        if (stored == null || stored.expiresAt().isBefore(LocalDateTime.now())) {
            emailCodes.remove(key);
            throw new ApiException(400, "验证码已过期或无效，请重新获取");
        }
        if (!stored.code().equals(code)) throw new ApiException(400, "验证码错误");
        emailCodes.remove(key);
    }

    public UserProfile currentUserProfile() {
        CurrentUser currentUser = AuthContext.currentUser();
        return identityService.findById(currentUser.getId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
    }

    private Optional<UserAccount> findUserByEmail(String email) { return identityService.findByEmail(normalizeEmail(email)); }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(); }
    private String codeKey(String email, AuthRequests.EmailCodePurpose purpose) { return purpose.name() + ":" + email; }
    private void validateCustomerEmail(String email) {
        if (!CUSTOMER_EMAIL_PATTERN.matcher(normalizeEmail(email)).matches()) throw new ApiException(400, "仅支持 QQ 邮箱和网易邮箱（163、126、yeah.net）");
    }
    private AuthResponses.UserVO toUserVO(UserAccount user) {
        var identityUser = UserConverter.toVO(user);
        return new AuthResponses.UserVO(identityUser.id(), identityUser.email(), identityUser.nickname(), identityUser.role(), identityUser.avatar());
    }
}
