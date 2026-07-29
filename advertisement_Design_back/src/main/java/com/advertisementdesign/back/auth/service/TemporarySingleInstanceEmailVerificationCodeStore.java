package com.advertisementdesign.back.auth.service;

import com.advertisementdesign.back.auth.dto.AuthRequests;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TemporarySingleInstanceEmailVerificationCodeStore implements EmailVerificationCodeStore {
    private final Map<String, StoredCode> codes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSentAt = new ConcurrentHashMap<>();

    @Override
    public Duration reserveSend(String email, LocalDateTime now, Duration throttle) {
        synchronized (lastSentAt) {
            LocalDateTime previous = lastSentAt.get(email);
            if (previous != null && previous.plus(throttle).isAfter(now)) {
                return Duration.between(now, previous.plus(throttle));
            }
            lastSentAt.put(email, now);
            return Duration.ZERO;
        }
    }

    @Override
    public void cancelSend(String email, LocalDateTime reservedAt) {
        synchronized (lastSentAt) {
            if (reservedAt.equals(lastSentAt.get(email))) {
                lastSentAt.remove(email);
            }
        }
    }

    @Override
    public void save(String email, AuthRequests.EmailCodePurpose purpose, String code, LocalDateTime expiresAt) {
        codes.put(key(email, purpose), new StoredCode(code, expiresAt));
    }

    @Override
    public boolean consume(String email, AuthRequests.EmailCodePurpose purpose, String code, LocalDateTime now) {
        String key = key(email, purpose);
        StoredCode stored = codes.get(key);
        if (stored == null || stored.expiresAt().isBefore(now) || !stored.code().equals(code)) {
            if (stored != null && stored.expiresAt().isBefore(now)) {
                codes.remove(key, stored);
            }
            return false;
        }
        return codes.remove(key, stored);
    }

    private String key(String email, AuthRequests.EmailCodePurpose purpose) {
        return purpose.name() + ":" + email;
    }

    private record StoredCode(String code, LocalDateTime expiresAt) {
    }
}
