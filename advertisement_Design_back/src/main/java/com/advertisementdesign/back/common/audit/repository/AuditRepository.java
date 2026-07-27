package com.advertisementdesign.back.common.audit.repository;

import com.advertisementdesign.back.common.audit.entity.OperationLogEntity;
import com.advertisementdesign.back.common.audit.mapper.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuditRepository {
    private final OperationLogMapper operationLogMapper;

    public List<OperationLogEntity> listByProject(Long projectId) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLogEntity>()
                .and(query -> query
                        .eq(OperationLogEntity::getBizType, "PROJECT")
                        .eq(OperationLogEntity::getBizId, projectId)
                        .or(stageQuery -> stageQuery
                                .eq(OperationLogEntity::getBizType, "STAGE")
                                .eq(OperationLogEntity::getBizId, projectId)))
                .orderByDesc(OperationLogEntity::getCreatedAt));
    }

    public OperationLogEntity save(OperationLogEntity operationLog) {
        if (operationLog.getCreatedAt() == null) {
            operationLog.setCreatedAt(LocalDateTime.now());
        }
        if (operationLog.getId() == null) {
            operationLogMapper.insert(operationLog);
        } else {
            operationLogMapper.updateById(operationLog);
        }
        return operationLog;
    }
}
