package com.advertisementdesign.back.workflow.converter;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.workflow.entity.ProjectStageEventEntity;
import com.advertisementdesign.back.workflow.entity.ProjectStageInstanceEntity;
import com.advertisementdesign.back.workflow.model.WorkflowModels;
import org.springframework.stereotype.Component;

@Component
public class WorkflowConverter {
    public WorkflowModels.StageInstanceView toStage(ProjectStageInstanceEntity entity) {
        return new WorkflowModels.StageInstanceView(
                entity.getId(), entity.getProjectId(), entity.getStageCode(), entity.getStageName(),
                entity.getSortOrder(), entity.getStatus(), entity.getActivationCount(), entity.getActivatedAt(),
                entity.getCompletedAt(), entity.getVersion());
    }

    public WorkflowModels.StageEventView toEvent(ProjectStageEventEntity entity) {
        return new WorkflowModels.StageEventView(
                entity.getId(), entity.getProjectId(), entity.getStageInstanceId(), entity.getStageCode(),
                entity.getEventType(), entity.getFromStatus(), entity.getToStatus(), entity.getActivationNumber(),
                entity.getRelatedObjectType(), entity.getRelatedObjectId(), entity.getRelatedObjectVersion(),
                entity.getReason(), entity.getOccurredAt());
    }

    public WorkflowModels.InternalStageEventView toInternalEvent(ProjectStageEventEntity entity) {
        ActorRef actor = new ActorRef(entity.getActorType(), entity.getActorId());
        return new WorkflowModels.InternalStageEventView(
                entity.getId(), entity.getProjectId(), entity.getStageInstanceId(), entity.getStageCode(),
                entity.getEventType(), entity.getFromStatus(), entity.getToStatus(), entity.getActivationNumber(),
                entity.getRelatedObjectType(), entity.getRelatedObjectId(), entity.getRelatedObjectVersion(), actor,
                entity.getSource(), entity.getAuthorizationBasis(), entity.getReason(), entity.getRequestId(),
                entity.getOccurredAt());
    }
}
