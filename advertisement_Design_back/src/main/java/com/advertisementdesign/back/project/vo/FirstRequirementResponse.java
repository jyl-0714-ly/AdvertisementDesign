package com.advertisementdesign.back.project.vo;

import com.advertisementdesign.back.workflow.enums.StageCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "首条需求处理结果")
public record FirstRequirementResponse(
        @Schema(description = "INVALID_REQUIREMENT / PROJECT_CREATED / IDEMPOTENT_REPLAY")
        Status status,
        Long projectId,
        Long conversationId,
        String projectName,
        StageCode currentStage,
        String guidance) {
    public enum Status {
        INVALID_REQUIREMENT,
        PROJECT_CREATED,
        IDEMPOTENT_REPLAY
    }
}
