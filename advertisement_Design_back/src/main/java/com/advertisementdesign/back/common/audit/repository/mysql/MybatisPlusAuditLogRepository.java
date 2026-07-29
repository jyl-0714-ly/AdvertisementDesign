package com.advertisementdesign.back.common.audit.repository.mysql;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;
import com.advertisementdesign.back.common.audit.mapper.AuditLogMapper;
import com.advertisementdesign.back.common.audit.repository.AuditLogRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MybatisPlusAuditLogRepository implements AuditLogRepository {
    private final AuditLogMapper mapper;

    @Override
    public AuditLogEntity append(AuditLogEntity auditLog) {
        if (auditLog.getId() != null) {
            throw new IllegalArgumentException("audit log is append-only");
        }
        if (auditLog.getOccurredAt() == null) {
            auditLog.setOccurredAt(LocalDateTime.now());
        }
        if (mapper.insert(auditLog) != 1) {
            throw new IllegalStateException("failed to append audit log");
        }
        return auditLog;
    }

    @Override
    public List<AuditLogEntity> findByProjectId(Long projectId) {
        return mapper.selectList(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getProjectId, projectId)
                .orderByDesc(AuditLogEntity::getOccurredAt)
                .orderByDesc(AuditLogEntity::getId));
    }
}
