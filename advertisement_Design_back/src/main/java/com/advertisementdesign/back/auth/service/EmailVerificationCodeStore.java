package com.advertisementdesign.back.auth.service;

import com.advertisementdesign.back.auth.dto.AuthRequests;

import java.time.Duration;
import java.time.LocalDateTime;

public interface EmailVerificationCodeStore {
    Duration reserveSend(String email, LocalDateTime now, Duration throttle);

    void cancelSend(String email, LocalDateTime reservedAt);

    void save(String email, AuthRequests.EmailCodePurpose purpose, String code, LocalDateTime expiresAt);

    boolean consume(String email, AuthRequests.EmailCodePurpose purpose, String code, LocalDateTime now);
}
