package com.advertisementdesign.back.project.model;

import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.project.enums.StageActionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "阶段动作相关模型")
public final class StageModels {
    private StageModels() {
    }

    @Schema(description = "创建阶段动作请求")
    public record CreateStageActionRequest(String requestNote) {
    }

    @Schema(description = "阶段动作响应请求")
    public record StageActionResponseRequest(String responseNote) {
    }

    @Schema(description = "阶段动作视图")
    public record StageActionVO(
            Long id,
            Long projectId,
            Long projectStageId,
            String stageCode,
            Long initiatorId,
            MessageSenderRole initiatorRole,
            Long confirmUserId,
            StageActionStatus status,
            String requestNote,
            String responseNote,
            String requestedAt,
            String respondedAt,
            String createdAt,
            String updatedAt
    ) {
    }
}
