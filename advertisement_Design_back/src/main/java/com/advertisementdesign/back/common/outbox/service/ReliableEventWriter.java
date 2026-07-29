package com.advertisementdesign.back.common.outbox.service;

import com.advertisementdesign.back.common.outbox.entity.OutboxEventEntity;
import com.advertisementdesign.back.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReliableEventWriter {
    private final OutboxEventRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Joins the caller transaction. The durable row is written now, while consumers are notified only after commit.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long write(Event event) {
        Objects.requireNonNull(event, "event");
        OutboxEventEntity persisted = repository.append(OutboxEventEntity.builder()
                .aggregateType(event.aggregateType())
                .aggregateId(event.aggregateId())
                .eventType(event.eventType())
                .eventKey(event.eventKey())
                .payload(event.payload())
                .build());
        eventPublisher.publishEvent(new OutboxReadyAfterCommit(persisted.getId()));
        return persisted.getId();
    }

    public record Event(String aggregateType, Long aggregateId, String eventType,
                        String eventKey, Map<String, Object> payload) {
    }

    public record OutboxReadyAfterCommit(Long outboxEventId) {
    }
}
