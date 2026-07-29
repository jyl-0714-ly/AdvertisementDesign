package com.advertisementdesign.back.workflow.model;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageEventType;
import com.advertisementdesign.back.workflow.enums.StageStatus;

import java.time.LocalDateTime;

public final class WorkflowModels {
    private WorkflowModels() {
    }

    public record StageDefinition(StageCode code, String name, int sortOrder, StageStatus initialStatus) {
    }

    public record StageInstanceView(
            Long id, Long projectId, StageCode stageCode, String stageName, Integer sortOrder,
            StageStatus status, Integer activationCount, LocalDateTime activatedAt,
            LocalDateTime completedAt, Long version) {
    }

    public record StageEventView(
            Long id, Long projectId, Long stageInstanceId, StageCode stageCode,
            StageEventType eventType, StageStatus fromStatus, StageStatus toStatus,
            Integer activationNumber, String relatedObjectType, Long relatedObjectId,
            Integer relatedObjectVersion, String reason, LocalDateTime occurredAt) {
    }

    public record InternalStageEventView(
            Long id, Long projectId, Long stageInstanceId, StageCode stageCode,
            StageEventType eventType, StageStatus fromStatus, StageStatus toStatus,
            Integer activationNumber, String relatedObjectType, Long relatedObjectId,
            Integer relatedObjectVersion, ActorRef actor, String authorizationBasis,
            String reason, String requestId, LocalDateTime occurredAt) {
    }
}
