package com.advertisementdesign.back.auth.service;

import com.advertisementdesign.back.auth.dto.AuthRequests;
import com.advertisementdesign.back.auth.security.JwtTokenService;
import com.advertisementdesign.back.auth.vo.AuthResponses;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
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
    private IdentityService identityService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private EmailCodeMailService emailCodeMailService;
    @Mock
    private CurrentActorProvider currentActorProvider;

    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;
    private TemporarySingleInstanceEmailVerificationCodeStore emailCodeStore;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        emailCodeStore = new TemporarySingleInstanceEmailVerificationCodeStore();
        authService = new AuthService(
                identityService,
                passwordEncoder,
                jwtTokenService,
                emailCodeMailService,
                emailCodeStore,
                currentActorProvider
        );
    }

    @Test
    void registerPersistsUserAndLoginReadsPersistedUser() {
        String email = "new-customer@163.com";
        String password = "123456";
        AtomicReference<UserAccount> persistedUser = new AtomicReference<>();

        when(identityService.findByEmail(any())).thenAnswer(invocation -> Optional.ofNullable(persistedUser.get()));
        when(identityService.createCustomer(any(), any(), any())).thenAnswer(invocation -> {
            UserAccount user = new UserAccount(100L, invocation.getArgument(0), null,
                    invocation.getArgument(1), invocation.getArgument(2), UserRole.CUSTOMER,
                    null, UserStatus.ENABLED);
            persistedUser.set(user);
            return user;
        });
        when(identityService.recordLogin(100L)).thenAnswer(invocation -> persistedUser.get());
        when(jwtTokenService.createToken(any())).thenReturn("test-token");

        authService.sendEmailCode(new AuthRequests.SendEmailCodeRequest(
                email,
                AuthRequests.EmailCodePurpose.REGISTER
        ));
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailCodeMailService).sendCode(eq(email), codeCaptor.capture());

        AuthResponses.UserVO registered = authService.register(new AuthRequests.RegisterRequest(
                email,
                codeCaptor.getValue(),
                password,
                "新客户"
        ));

        assertEquals(100L, registered.id());
        assertEquals(email, persistedUser.get().email());
        assertEquals(UserRole.CUSTOMER, persistedUser.get().role());
        assertTrue(passwordEncoder.matches(password, persistedUser.get().passwordHash()));

        AuthResponses.LoginResponse login = authService.login(new AuthRequests.LoginRequest(email, password));

        assertEquals("test-token", login.token());
        assertNotNull(login.user());
        assertEquals(100L, login.user().id());
        verify(identityService).recordLogin(100L);
    }
}
