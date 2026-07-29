package com.advertisementdesign.back.common.audit.repository;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;

import java.util.List;

/** Append-only audit boundary. Deliberately exposes no ordinary update or delete operation. */
public interface AuditLogRepository {
    AuditLogEntity append(AuditLogEntity auditLog);

    List<AuditLogEntity> findByProjectId(Long projectId);
}
