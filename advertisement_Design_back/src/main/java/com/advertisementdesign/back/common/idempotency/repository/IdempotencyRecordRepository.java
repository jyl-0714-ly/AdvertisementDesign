package com.advertisementdesign.back.common.idempotency.repository;

import com.advertisementdesign.back.common.idempotency.entity.IdempotencyRecordEntity;
import com.advertisementdesign.back.identity.model.ActorRef;

import java.util.Optional;

public interface IdempotencyRecordRepository {
    boolean tryInsert(IdempotencyRecordEntity record);

    Optional<IdempotencyRecordEntity> find(ActorRef actor, String operationType, String idempotencyKey);

    boolean markSucceeded(Long id, long expectedVersion, String resourceType, Long resourceId,
                          java.util.Map<String, Object> responseSnapshot);

    boolean markFailed(Long id, long expectedVersion, String failureCode);

    boolean retryFailed(Long id, long expectedVersion);
}
