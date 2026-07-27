package com.advertisementdesign.back.common.audit.service;

import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.audit.converter.OperationLogConverter;
import com.advertisementdesign.back.common.audit.model.OperationLogModels;
import com.advertisementdesign.back.common.audit.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final AuditRepository auditRepository;
    private final OperationLogConverter converter;

    public PageResult<OperationLogModels.OperationLogVO> list(
            Long projectId,
            String bizType,
            String action,
            long page,
            long size) {
        var filtered = auditRepository.listByProject(projectId).stream()
                .filter(log -> bizType == null || bizType.isBlank()
                        || bizType.equalsIgnoreCase(log.getBizType()))
                .filter(log -> action == null || action.isBlank()
                        || action.equalsIgnoreCase(log.getAction()))
                .map(converter::toVO)
                .toList();
        long from = Math.max(page - 1, 0) * size;
        var records = filtered.stream().skip(from).limit(size).toList();
        return PageResult.of(records, filtered.size(), page, size);
    }
}
