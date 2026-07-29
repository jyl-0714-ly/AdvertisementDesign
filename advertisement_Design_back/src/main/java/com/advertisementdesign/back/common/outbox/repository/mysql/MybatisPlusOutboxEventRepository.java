package com.advertisementdesign.back.common.outbox.repository.mysql;

import com.advertisementdesign.back.common.outbox.entity.OutboxEventEntity;
import com.advertisementdesign.back.common.outbox.mapper.OutboxEventMapper;
import com.advertisementdesign.back.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class MybatisPlusOutboxEventRepository implements OutboxEventRepository {
    private final OutboxEventMapper mapper;

    @Override
    public OutboxEventEntity append(OutboxEventEntity event) {
        if (event.getId() != null) {
            throw new IllegalArgumentException("new outbox event must not have an id");
        }
        LocalDateTime now = LocalDateTime.now();
        if (event.getStatus() == null) event.setStatus(OutboxEventEntity.Status.PENDING);
        if (event.getAvailableAt() == null) event.setAvailableAt(now);
        if (event.getRetryCount() == null) event.setRetryCount(0);
        if (event.getVersion() == null) event.setVersion(0L);
        if (event.getCreatedAt() == null) event.setCreatedAt(now);
        if (event.getUpdatedAt() == null) event.setUpdatedAt(now);
        if (mapper.insert(event) != 1) {
            throw new IllegalStateException("failed to append outbox event");
        }
        return event;
    }
}
