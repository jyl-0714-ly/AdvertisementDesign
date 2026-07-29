package com.advertisementdesign.back.common.reliability;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;
import com.advertisementdesign.back.common.audit.mapper.AuditLogMapper;
import com.advertisementdesign.back.common.audit.repository.AuditLogRepository;
import com.advertisementdesign.back.common.idempotency.mapper.IdempotencyRecordMapper;
import com.advertisementdesign.back.common.idempotency.service.IdempotencyService;
import com.advertisementdesign.back.common.outbox.mapper.OutboxEventMapper;
import com.advertisementdesign.back.common.outbox.service.PostCommitOutboxBoundary;
import com.advertisementdesign.back.common.outbox.service.ReliableEventWriter;
import com.advertisementdesign.back.identity.model.ActorRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.main.lazy-initialization=true",
        "spring.sql.init.mode=always"
})
@ActiveProfiles("test")
class ReliabilityFoundationIntegrationTest {
    @Autowired private IdempotencyService idempotencyService;
    @Autowired private IdempotencyRecordMapper idempotencyMapper;
    @Autowired private AuditLogRepository auditRepository;
    @Autowired private AuditLogMapper auditMapper;
    @Autowired private ReliableEventWriter eventWriter;
    @Autowired private OutboxEventMapper outboxMapper;
    @Autowired private PostCommitOutboxBoundary postCommitBoundary;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void concurrentDuplicateIdempotencyHasAtMostOneOwnerAndOneStoredResult() throws Exception {
        int attempts = 8;
        var pool = Executors.newFixedThreadPool(attempts);
        var ready = new CountDownLatch(attempts);
        var start = new CountDownLatch(1);
        var actor = new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 9001L);
        var key = new IdempotencyService.CommandKey(actor, "TEST_CONCURRENT_COMMAND", "same-client-key");
        try {
            List<java.util.concurrent.Future<IdempotencyService.Claim>> futures = java.util.stream.IntStream.range(0, attempts)
                    .mapToObj(index -> pool.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        return idempotencyService.claim(key, "same-request-hash", null);
                    })).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<IdempotencyService.Claim> claims = futures.stream().map(future -> {
                try {
                    return future.get(10, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }).toList();
            List<IdempotencyService.Claim> owners = claims.stream()
                    .filter(claim -> claim.state() == IdempotencyService.ClaimState.OWNER).toList();
            assertEquals(1, owners.size());
            idempotencyService.succeed(owners.get(0), "TEST_RESOURCE", 77L, null);
            assertEquals(1L, idempotencyMapper.selectCount(null));
            assertEquals(IdempotencyService.ClaimState.REPLAY_SUCCEEDED,
                    idempotencyService.claim(key, "same-request-hash", null).state());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rollbackRemovesAuditAndOutboxAndDoesNotCrossPostCommitBoundary() throws Exception {
        AtomicInteger delivered = new AtomicInteger();
        try (AutoCloseable ignored = postCommitBoundary.register(id -> delivered.incrementAndGet())) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                auditRepository.append(AuditLogEntity.builder()
                        .actorType(ActorRef.ActorType.SYSTEM_EVENT).source(AuditLogEntity.Source.SYSTEM)
                        .objectType("TEST_OBJECT").action("TEST_ROLLBACK").result(AuditLogEntity.Result.SUCCESS)
                        .requestId("rollback-request").build());
                eventWriter.write(new ReliableEventWriter.Event(
                        "TEST", 1L, "TEST_FUTURE_ACTION", "rollback-event", Map.of("safe", true)));
                status.setRollbackOnly();
            });
        }
        assertEquals(0L, auditMapper.selectCount(null));
        assertEquals(0L, outboxMapper.selectCount(null));
        assertEquals(0, delivered.get());
    }

    @Test
    void auditRepositoryApiIsAppendOnly() {
        List<String> publicMethods = Arrays.stream(AuditLogRepository.class.getDeclaredMethods())
                .map(Method::getName).toList();
        assertTrue(publicMethods.contains("append"));
        assertTrue(publicMethods.contains("findByProjectId"));
        assertFalse(publicMethods.stream().anyMatch(name -> name.startsWith("update")
                || name.startsWith("delete") || name.equals("save")));
    }
}
