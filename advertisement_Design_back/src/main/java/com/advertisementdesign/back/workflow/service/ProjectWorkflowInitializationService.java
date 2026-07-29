package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.workflow.entity.ProjectStageEventEntity;
import com.advertisementdesign.back.workflow.entity.ProjectStageInstanceEntity;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageEventSource;
import com.advertisementdesign.back.workflow.enums.StageEventType;
import com.advertisementdesign.back.workflow.enums.StageStatus;
import com.advertisementdesign.back.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectWorkflowInitializationService {
    private final FixedStageCatalog catalog;
    private final WorkflowRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public InitializedWorkflow initialize(Command command) {
        List<ProjectStageInstanceEntity> stages = catalog.createInitialInstances(command.projectId(), command.occurredAt());
        ProjectStageInstanceEntity current = stages.stream()
                .filter(stage -> stage.getStageCode() == StageCode.REQUIREMENT_GUIDE)
                .findFirst().orElseThrow();
        current.setStatus(StageStatus.ACTIVE);
        current.setActivationCount(1);
        current.setActivatedAt(command.occurredAt());
        repository.insertInitialStages(stages);
        repository.appendEvent(ProjectStageEventEntity.builder()
                .projectId(command.projectId())
                .stageInstanceId(current.getId())
                .stageCode(StageCode.REQUIREMENT_GUIDE)
                .eventType(StageEventType.ACTIVATED)
                .fromStatus(StageStatus.NOT_STARTED)
                .toStatus(StageStatus.ACTIVE)
                .activationNumber(1)
                .actorType(command.actor().type())
                .actorId(command.actor().actorId())
                .source(StageEventSource.CUSTOMER_UI)
                .authorizationBasis(command.authorizationBasis())
                .reason("首条有效设计需求已提交")
                .requestId(command.requestId())
                .occurredAt(command.occurredAt())
                .build());
        return new InitializedWorkflow(current.getId(), StageCode.REQUIREMENT_GUIDE, current.getStageName());
    }

    public record Command(Long projectId, ActorRef actor, String authorizationBasis,
                          String requestId, LocalDateTime occurredAt) {
    }

    public record InitializedWorkflow(Long stageInstanceId, StageCode stageCode, String stageName) {
    }
}
