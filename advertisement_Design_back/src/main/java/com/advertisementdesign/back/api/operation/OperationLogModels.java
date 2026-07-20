package com.advertisementdesign.back.api.operation;

import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "操作日志相关模型")
public final class OperationLogModels {
    private OperationLogModels() {
    }

    @Schema(description = "操作日志视图")
    public record OperationLogVO(
            Long id,
            Long operatorId,
            MessageSenderRole operatorRole,
            String bizType,
            Long bizId,
            String action,
            String description,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String createdAt
    ) {
    }
}
