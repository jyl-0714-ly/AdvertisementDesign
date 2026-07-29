package com.advertisementdesign.back.common.idempotency.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.idempotency.entity.IdempotencyRecordEntity;
import com.advertisementdesign.back.common.idempotency.repository.IdempotencyRecordRepository;
import com.advertisementdesign.back.identity.model.ActorRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRecordRepository repository;

    @Transactional
    public Claim claim(CommandKey key, String requestHash, LocalDateTime expiresAt) {
        requireKey(key, requestHash);
        IdempotencyRecordEntity candidate = IdempotencyRecordEntity.builder()
                .operationType(key.operationType())
                .actorType(key.actor().type())
                .actorId(key.actor().actorId())
                .idempotencyKey(key.idempotencyKey())
                .requestHash(requestHash)
                .status(IdempotencyRecordEntity.Status.PROCESSING)
                .expiresAt(expiresAt)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        if (repository.tryInsert(candidate)) {
            return new Claim(candidate.getId(), candidate.getVersion(), ClaimState.OWNER, null, null, null);
        }
        IdempotencyRecordEntity existing = repository.find(key.actor(), key.operationType(), key.idempotencyKey())
                .orElseThrow(() -> conflict("幂等请求并发处理中，请稍后重试"));
        if (!existing.getRequestHash().equals(requestHash)) {
            throw conflict("同一幂等键不能用于不同请求");
        }
        if (existing.getStatus() == IdempotencyRecordEntity.Status.SUCCEEDED) {
            return new Claim(existing.getId(), existing.getVersion(), ClaimState.REPLAY_SUCCEEDED,
                    existing.getResourceId(), existing.getResponseSnapshot(), null);
        }
        if (existing.getStatus() == IdempotencyRecordEntity.Status.PROCESSING) {
            return new Claim(existing.getId(), existing.getVersion(), ClaimState.IN_PROGRESS, null, null, null);
        }
        if (repository.retryFailed(existing.getId(), existing.getVersion())) {
            return new Claim(existing.getId(), existing.getVersion() + 1, ClaimState.OWNER, null, null, null);
        }
        return new Claim(existing.getId(), existing.getVersion(), ClaimState.IN_PROGRESS, null, null, null);
    }

    @Transactional
    public void succeed(Claim owner, String resourceType, Long resourceId, Map<String, Object> responseSnapshot) {
        requireOwner(owner);
        if (!repository.markSucceeded(owner.recordId(), owner.version(), resourceType, resourceId, responseSnapshot)) {
            throw conflict("幂等请求状态已被其他操作更新");
        }
    }

    @Transactional
    public void fail(Claim owner, String failureCode) {
        requireOwner(owner);
        if (!repository.markFailed(owner.recordId(), owner.version(), failureCode)) {
            throw conflict("幂等请求状态已被其他操作更新");
        }
    }

    private void requireKey(CommandKey key, String requestHash) {
        Objects.requireNonNull(key, "key");
        if (key.operationType() == null || key.operationType().isBlank()
                || key.idempotencyKey() == null || key.idempotencyKey().isBlank()
                || requestHash == null || requestHash.isBlank()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "幂等命令信息不完整");
        }
    }

    private void requireOwner(Claim claim) {
        if (claim == null || claim.state() != ClaimState.OWNER) {
            throw conflict("当前调用不是幂等请求执行者");
        }
    }

    private ApiException conflict(String message) {
        return new ApiException(ApiErrorCode.CONFLICT.getCode(), message);
    }

    public record CommandKey(ActorRef actor, String operationType, String idempotencyKey) {
        public CommandKey {
            Objects.requireNonNull(actor, "actor");
        }
    }

    public record Claim(Long recordId, long version, ClaimState state, Long resourceId,
                        Map<String, Object> responseSnapshot, String failureCode) {
    }

    public enum ClaimState {
        OWNER,
        IN_PROGRESS,
        REPLAY_SUCCEEDED
    }
}
