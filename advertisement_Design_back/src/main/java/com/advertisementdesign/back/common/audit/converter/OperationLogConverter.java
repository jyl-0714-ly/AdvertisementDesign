package com.advertisementdesign.back.common.audit.converter;

import com.advertisementdesign.back.common.audit.entity.OperationLogEntity;
import com.advertisementdesign.back.common.audit.model.OperationLogModels;
import org.springframework.stereotype.Component;

@Component
public class OperationLogConverter {
    public OperationLogModels.OperationLogVO toVO(OperationLogEntity entity) {
        return new OperationLogModels.OperationLogVO(
                entity.getId(),
                entity.getOperatorId(),
                entity.getOperatorRole(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getAction(),
                entity.getDescription(),
                entity.getBeforeData(),
                entity.getAfterData(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString()
        );
    }
}
