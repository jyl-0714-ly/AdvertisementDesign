package com.advertisementdesign.back.common.idempotency.repository.mysql;

import com.advertisementdesign.back.common.idempotency.entity.IdempotencyRecordEntity;
import com.advertisementdesign.back.common.idempotency.mapper.IdempotencyRecordMapper;
import com.advertisementdesign.back.common.idempotency.repository.IdempotencyRecordRepository;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisPlusIdempotencyRecordRepository implements IdempotencyRecordRepository {
    private final IdempotencyRecordMapper mapper;

    @Override
    public boolean tryInsert(IdempotencyRecordEntity record) {
        try {
            return mapper.insert(record) == 1;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    @Override
    public Optional<IdempotencyRecordEntity> find(ActorRef actor, String operationType, String idempotencyKey) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<IdempotencyRecordEntity>()
                .eq(IdempotencyRecordEntity::getActorType, actor.type())
                .eq(IdempotencyRecordEntity::getActorId, actor.actorId())
                .eq(IdempotencyRecordEntity::getOperationType, operationType)
                .eq(IdempotencyRecordEntity::getIdempotencyKey, idempotencyKey)));
    }

    @Override
    public boolean markSucceeded(Long id, long expectedVersion, String resourceType, Long resourceId,
                                 Map<String, Object> responseSnapshot) {
        IdempotencyRecordEntity changes = IdempotencyRecordEntity.builder()
                .status(IdempotencyRecordEntity.Status.SUCCEEDED)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .responseSnapshot(responseSnapshot)
                .updatedAt(LocalDateTime.now())
                .version(expectedVersion + 1)
                .build();
        return mapper.update(changes, processingCondition(id, expectedVersion)) == 1;
    }

    @Override
    public boolean markFailed(Long id, long expectedVersion, String failureCode) {
        IdempotencyRecordEntity changes = IdempotencyRecordEntity.builder()
                .status(IdempotencyRecordEntity.Status.FAILED)
                .failureCode(failureCode)
                .updatedAt(LocalDateTime.now())
                .version(expectedVersion + 1)
                .build();
        return mapper.update(changes, processingCondition(id, expectedVersion)) == 1;
    }

    @Override
    public boolean retryFailed(Long id, long expectedVersion) {
        return mapper.update(null, new LambdaUpdateWrapper<IdempotencyRecordEntity>()
                .eq(IdempotencyRecordEntity::getId, id)
                .eq(IdempotencyRecordEntity::getVersion, expectedVersion)
                .eq(IdempotencyRecordEntity::getStatus, IdempotencyRecordEntity.Status.FAILED)
                .set(IdempotencyRecordEntity::getStatus, IdempotencyRecordEntity.Status.PROCESSING)
                .set(IdempotencyRecordEntity::getFailureCode, null)
                .set(IdempotencyRecordEntity::getUpdatedAt, LocalDateTime.now())
                .set(IdempotencyRecordEntity::getVersion, expectedVersion + 1)) == 1;
    }

    private LambdaUpdateWrapper<IdempotencyRecordEntity> processingCondition(Long id, long expectedVersion) {
        return new LambdaUpdateWrapper<IdempotencyRecordEntity>()
                .eq(IdempotencyRecordEntity::getId, id)
                .eq(IdempotencyRecordEntity::getVersion, expectedVersion)
                .eq(IdempotencyRecordEntity::getStatus, IdempotencyRecordEntity.Status.PROCESSING);
    }
}
