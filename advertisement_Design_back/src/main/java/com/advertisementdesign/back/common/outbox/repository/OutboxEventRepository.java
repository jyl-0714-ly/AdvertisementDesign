package com.advertisementdesign.back.common.outbox.repository;

import com.advertisementdesign.back.common.outbox.entity.OutboxEventEntity;

public interface OutboxEventRepository {
    OutboxEventEntity append(OutboxEventEntity event);
}
