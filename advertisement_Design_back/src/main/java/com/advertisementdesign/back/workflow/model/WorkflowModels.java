package com.advertisementdesign.back.workflow.model;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageEventSource;
import com.advertisementdesign.back.workflow.enums.StageEventType;
import com.advertisementdesign.back.workflow.enums.StageStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

    public record CurrentStageWorkspaceView(
            StageInstanceView stage,
            Set<String> allowedActions,
            List<Object> versions,
            List<Object> materials
    ) {
        public CurrentStageWorkspaceView {
            allowedActions = allowedActions == null ? Set.of() : Set.copyOf(allowedActions);
            versions = versions == null ? List.of() : List.copyOf(versions);
            materials = materials == null ? List.of() : List.copyOf(materials);
        }
    }

    public record WorkflowCommandResult(
            StageInstanceView stage,
            StageEventView event,
            boolean idempotentReplay
    ) {
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
            Integer relatedObjectVersion, ActorRef actor, StageEventSource source,
            String authorizationBasis,
            String reason, String requestId, LocalDateTime occurredAt) {
    }
}
