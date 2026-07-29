package com.advertisementdesign.back.common.outbox.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Minimal in-process post-commit boundary. Future dispatch infrastructure can register a wake-up consumer;
 * durable truth remains the outbox row, so a missed wake-up is safe for later polling.
 */
@Component
public class PostCommitOutboxBoundary {
    private final List<Consumer<Long>> consumers = new CopyOnWriteArrayList<>();

    public AutoCloseable register(Consumer<Long> consumer) {
        consumers.add(consumer);
        return () -> consumers.remove(consumer);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(ReliableEventWriter.OutboxReadyAfterCommit event) {
        notifyConsumers(event.outboxEventId());
    }

    @EventListener
    public void withoutTransaction(ReliableEventWriter.OutboxReadyAfterCommit event) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            notifyConsumers(event.outboxEventId());
        }
    }

    private void notifyConsumers(Long outboxEventId) {
        consumers.forEach(consumer -> consumer.accept(outboxEventId));
    }
}
