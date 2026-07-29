package com.advertisementdesign.back.common.audit.service;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;
import com.advertisementdesign.back.common.audit.repository.AuditLogRepository;
import com.advertisementdesign.back.identity.model.ActorRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogWriter {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long append(Entry entry) {
        AuditLogEntity log = repository.append(AuditLogEntity.builder()
                .projectId(entry.projectId())
                .actorType(entry.actor().type())
                .actorId(entry.actor().actorId())
                .customerDisplayIdentity(entry.customerDisplayIdentity())
                .source(entry.source())
                .objectType(entry.objectType())
                .objectId(entry.objectId())
                .objectVersion(entry.objectVersion())
                .action(entry.action())
                .authorizationBasis(entry.authorizationBasis())
                .beforeState(entry.beforeState())
                .afterState(entry.afterState())
                .result(entry.result())
                .failureCode(entry.failureCode())
                .requestId(entry.requestId())
                .correlationId(entry.correlationId())
                .occurredAt(entry.occurredAt())
                .build());
        return log.getId();
    }

    public record Entry(Long projectId, ActorRef actor, String customerDisplayIdentity,
                        AuditLogEntity.Source source, String objectType, Long objectId, String objectVersion,
                        String action, Map<String, Object> authorizationBasis, Map<String, Object> beforeState,
                        Map<String, Object> afterState, AuditLogEntity.Result result, String failureCode,
                        String requestId, String correlationId, LocalDateTime occurredAt) {
    }
}
