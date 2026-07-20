package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.operation.OperationLogModels;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.domain.entity.OperationLogEntity;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final DemoDataStore store;
    private final ApiAssembler assembler;

    public PageResult<OperationLogModels.OperationLogVO> list(Long projectId, String bizType, String action, long page, long size) {
        var filtered = store.listOperationLogs(projectId).stream()
                .filter(log -> bizType == null || bizType.isBlank() || bizType.equalsIgnoreCase(log.getBizType()))
                .filter(log -> action == null || action.isBlank() || action.equalsIgnoreCase(log.getAction()))
                .map(assembler::toOperationLogVO)
                .toList();
        ListWrapper<OperationLogModels.OperationLogVO> wrapper = new ListWrapper<>(filtered);
        long from = Math.max(page - 1, 0) * size;
        var records = filtered.stream().skip(from).limit(size).toList();
        return PageResult.of(records, filtered.size(), page, size);
    }

    private record ListWrapper<T>(java.util.List<T> records) {
    }
}
