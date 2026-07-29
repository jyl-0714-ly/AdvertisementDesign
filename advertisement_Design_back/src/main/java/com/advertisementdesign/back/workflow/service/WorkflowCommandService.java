package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.outbox.service.ReliableEventWriter;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.advertisementdesign.back.workflow.converter.WorkflowConverter;
import com.advertisementdesign.back.workflow.dto.WorkflowCommandRequests;
import com.advertisementdesign.back.workflow.entity.ProjectStageEventEntity;
import com.advertisementdesign.back.workflow.entity.ProjectStageInstanceEntity;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageEventSource;
import com.advertisementdesign.back.workflow.enums.StageEventType;
import com.advertisementdesign.back.workflow.enums.StageStatus;
import com.advertisementdesign.back.workflow.enums.WorkflowCommandType;
import com.advertisementdesign.back.workflow.model.WorkflowModels;
import com.advertisementdesign.back.workflow.repository.WorkflowRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowCommandService {
    private static final EnumSet<StageStatus> ORDINARY_ACTIVE_STATES = EnumSet.of(
            StageStatus.ACTIVE, StageStatus.WAITING_CUSTOMER, StageStatus.PROCESSING,
            StageStatus.UNDER_REVIEW, StageStatus.CHANGE_PROCESSING);

    private final WorkflowRepository repository;
    private final WorkflowConverter converter;
    private final ProjectQueryService projectQueryService;
    private final ProjectAuthorizationService authorizationService;
    private final CurrentActorProvider currentActorProvider;
    private final List<WorkflowStageGate> stageGates;
    private final ReliableEventWriter eventWriter;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowModels.WorkflowCommandResult execute(
            Long projectId,
            Long stageInstanceId,
            WorkflowCommandRequests.Execute request
    ) {
        projectQueryService.findContext(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ProjectStageInstanceEntity stage = repository.findStage(projectId, stageInstanceId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));

        ProjectAuthorizationService.AuthorizationDecision authorization =
                authorize(projectId, stage, request.command());

        ProjectStageEventEntity replay = repository.findEventByRequestId(request.requestId()).orElse(null);
        if (replay != null) {
            if (!replay.getProjectId().equals(projectId)
                    || !replay.getStageInstanceId().equals(stageInstanceId)
                    || replay.getEventType() != eventType(request.command())
                    || !java.util.Objects.equals(replay.getRelatedObjectType(), request.relatedObjectType())
                    || !java.util.Objects.equals(replay.getRelatedObjectId(), request.relatedObjectId())
                    || !java.util.Objects.equals(replay.getRelatedObjectVersion(), request.relatedObjectVersion())) {
                throw conflict("幂等请求标识已用于其他工作流命令");
            }
            return new WorkflowModels.WorkflowCommandResult(
                    converter.toStage(stage), converter.toEvent(replay), true);
        }

        if (!stage.getVersion().equals(request.version())) {
            throw conflict("阶段已被其他操作更新，请刷新后重试");
        }

        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        LocalDateTime now = LocalDateTime.now();
        StageStatus target = targetStatus(stage, request.command());

        verifyPrerequisiteOrder(projectId, stage, request.command());
        verifyBusinessGate(projectId, stage, request);

        if (request.command() == WorkflowCommandType.REOPEN) {
            suspendLaterCurrentStage(projectId, stage, actor, authorization, request, now);
        }

        int activationRound = activationRound(stage, request.command());
        LocalDateTime activatedAt = activates(request.command()) ? now : null;
        LocalDateTime completedAt = target == StageStatus.COMPLETED ? now : null;
        if (!repository.transition(projectId, stage.getId(), request.version(), stage.getStatus(), target,
                activationRound, activatedAt, completedAt, now)) {
            throw conflict("阶段状态发生并发变化，请刷新后重试");
        }

        ProjectStageEventEntity event = appendEvent(stage, target, activationRound, actor,
                source(actor), authorization, request, now);
        applyInMemory(stage, target, activationRound, activatedAt, completedAt, now);

        if (request.command() == WorkflowCommandType.COMPLETE) {
            activateNextStage(projectId, stage, actor, authorization, request, now);
        }

        eventWriter.write(new ReliableEventWriter.Event(
                "PROJECT_WORKFLOW", projectId, "PROJECT_STAGE_CHANGED", request.requestId() + ":after-commit",
                Map.of("stageInstanceId", stage.getId(), "stageCode", stage.getStageCode().name(),
                        "eventType", event.getEventType().name(), "activationRound", activationRound)));

        return new WorkflowModels.WorkflowCommandResult(converter.toStage(stage), converter.toEvent(event), false);
    }

    private ProjectAuthorizationService.AuthorizationDecision authorize(
            Long projectId, ProjectStageInstanceEntity stage, WorkflowCommandType command) {
        ProjectAuthorizationService.ProjectAction action = command == WorkflowCommandType.COMPLETE
                ? completionAction(stage.getStageCode())
                : ProjectAuthorizationService.ProjectAction.UPDATE_PROJECT;
        ProjectAuthorizationService.AuthorizationDecision decision = authorizationService.authorize(projectId, action);
        if (!decision.allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return decision;
    }

    private ProjectAuthorizationService.ProjectAction completionAction(StageCode stageCode) {
        return switch (stageCode) {
            case REQUIREMENT_GUIDE -> ProjectAuthorizationService.ProjectAction.CONFIRM_REQUIREMENT;
            case CONTRACT_PREPAYMENT -> ProjectAuthorizationService.ProjectAction.MANAGE_PAYMENT;
            case RESEARCH_REPORT -> ProjectAuthorizationService.ProjectAction.CONFIRM_REPORT;
            case SKETCH_STYLE, REVIEW_FINAL -> ProjectAuthorizationService.ProjectAction.CONFIRM_DESIGN;
            case DELIVERY_FINAL_PAYMENT -> ProjectAuthorizationService.ProjectAction.RECEIVE_DELIVERY;
            case AFTER_SALE_REPURCHASE -> ProjectAuthorizationService.ProjectAction.UPDATE_PROJECT;
        };
    }

    private StageStatus targetStatus(ProjectStageInstanceEntity stage, WorkflowCommandType command) {
        StageStatus current = stage.getStatus();
        if (current == StageStatus.SUSPENDED && command != WorkflowCommandType.RESUME) {
            throw conflict("阶段已暂停，只能先执行恢复命令");
        }
        return switch (command) {
            case ACTIVATE -> requireCurrent(current, EnumSet.of(StageStatus.NOT_STARTED), StageStatus.ACTIVE);
            case START_PROCESSING -> requireCurrent(current, ORDINARY_ACTIVE_STATES, StageStatus.PROCESSING);
            case WAIT_FOR_CUSTOMER -> requireCurrent(current, ORDINARY_ACTIVE_STATES, StageStatus.WAITING_CUSTOMER);
            case REQUEST_REVIEW -> requireCurrent(current, ORDINARY_ACTIVE_STATES, StageStatus.UNDER_REVIEW);
            case COMPLETE -> requireCurrent(current, ORDINARY_ACTIVE_STATES, StageStatus.COMPLETED);
            case SUSPEND -> requireCurrent(current, ORDINARY_ACTIVE_STATES, StageStatus.SUSPENDED);
            case RESUME -> resumeTarget(stage);
            case REOPEN -> requireCurrent(current, EnumSet.of(StageStatus.COMPLETED), StageStatus.ACTIVE);
        };
    }

    private StageStatus requireCurrent(StageStatus current, EnumSet<StageStatus> allowed, StageStatus target) {
        if (!allowed.contains(current) || current == target) {
            throw conflict("当前阶段状态不允许执行该命令");
        }
        return target;
    }

    private StageStatus resumeTarget(ProjectStageInstanceEntity stage) {
        if (stage.getStatus() != StageStatus.SUSPENDED) {
            throw conflict("只有已暂停阶段可以恢复");
        }
        return repository.findEvents(stage.getProjectId(), stage.getId()).stream()
                .filter(event -> event.getEventType() == StageEventType.SUSPENDED)
                .reduce((first, second) -> second)
                .map(ProjectStageEventEntity::getFromStatus)
                .filter(ORDINARY_ACTIVE_STATES::contains)
                .orElse(StageStatus.ACTIVE);
    }

    private void verifyPrerequisiteOrder(Long projectId, ProjectStageInstanceEntity stage, WorkflowCommandType command) {
        if (command != WorkflowCommandType.ACTIVATE
                && command != WorkflowCommandType.RESUME
                && command != WorkflowCommandType.REOPEN) {
            return;
        }
        boolean priorIncomplete = repository.findStages(projectId).stream()
                .anyMatch(item -> item.getSortOrder() < stage.getSortOrder()
                        && item.getStatus() != StageStatus.COMPLETED);
        if (priorIncomplete) {
            throw new ApiException(ApiErrorCode.BUSINESS_ERROR.getCode(), "前置阶段尚未完成，不能激活当前阶段");
        }
    }

    private void verifyBusinessGate(Long projectId, ProjectStageInstanceEntity stage,
                                    WorkflowCommandRequests.Execute request) {
        if (request.command() != WorkflowCommandType.COMPLETE) {
            return;
        }
        WorkflowStageGate gate = stageGates.stream()
                .filter(candidate -> candidate.supports(stage.getStageCode()))
                .findFirst()
                .orElse(null);
        if (gate == null && stage.getStageCode() != StageCode.AFTER_SALE_REPURCHASE) {
            throw new ApiException(ApiErrorCode.BUSINESS_ERROR.getCode(), "当前阶段尚未接入真实业务完成门槛");
        }
        if (gate != null) {
            gate.verify(new WorkflowStageGate.Context(projectId, stage, request.command(),
                    request.relatedObjectType(), request.relatedObjectId(), request.relatedObjectVersion()));
        }
    }

    private void suspendLaterCurrentStage(Long projectId, ProjectStageInstanceEntity reopened,
                                          ActorRef actor,
                                          ProjectAuthorizationService.AuthorizationDecision authorization,
                                          WorkflowCommandRequests.Execute request, LocalDateTime now) {
        ProjectStageInstanceEntity later = repository.findStages(projectId).stream()
                .filter(item -> item.getSortOrder() > reopened.getSortOrder())
                .filter(item -> ORDINARY_ACTIVE_STATES.contains(item.getStatus()))
                .findFirst().orElse(null);
        if (later == null) {
            return;
        }
        if (!repository.transition(projectId, later.getId(), later.getVersion(), later.getStatus(),
                StageStatus.SUSPENDED, null, null, null, now)) {
            throw conflict("后续阶段发生并发变化，无法安全重开");
        }
        WorkflowCommandRequests.Execute suspension = new WorkflowCommandRequests.Execute(
                WorkflowCommandType.SUSPEND, later.getVersion(), request.requestId() + ":later-suspended",
                "较早阶段重开，后续活动阶段已暂停", "STAGE_INSTANCE", reopened.getId(), reopened.getVersion().intValue());
        appendEvent(later, StageStatus.SUSPENDED, later.getActivationCount(), actor,
                source(actor), authorization, suspension, now);
    }

    private void activateNextStage(Long projectId, ProjectStageInstanceEntity completed,
                                   ActorRef actor,
                                   ProjectAuthorizationService.AuthorizationDecision authorization,
                                   WorkflowCommandRequests.Execute request, LocalDateTime now) {
        if (completed.getStageCode() == StageCode.AFTER_SALE_REPURCHASE) {
            return;
        }
        ProjectStageInstanceEntity next = repository.findStages(projectId).stream()
                .filter(item -> item.getSortOrder() == completed.getSortOrder() + 1)
                .findFirst().orElseThrow();
        if (next.getStatus() != StageStatus.NOT_STARTED) {
            return;
        }
        int nextRound = next.getActivationCount() + 1;
        if (!repository.transition(projectId, next.getId(), next.getVersion(), StageStatus.NOT_STARTED,
                StageStatus.ACTIVE, nextRound, now, null, now)) {
            throw conflict("下一阶段发生并发变化，无法完成当前阶段");
        }
        WorkflowCommandRequests.Execute activation = new WorkflowCommandRequests.Execute(
                WorkflowCommandType.ACTIVATE, next.getVersion(), request.requestId() + ":next-activated",
                "前置阶段完成后激活", "STAGE_INSTANCE", completed.getId(), completed.getVersion().intValue());
        appendEvent(next, StageStatus.ACTIVE, nextRound, actor, source(actor), authorization, activation, now);
    }

    private ProjectStageEventEntity appendEvent(ProjectStageInstanceEntity stage, StageStatus target,
                                                 int activationRound, ActorRef actor, StageEventSource source,
                                                 ProjectAuthorizationService.AuthorizationDecision authorization,
                                                 WorkflowCommandRequests.Execute request, LocalDateTime now) {
        return repository.appendEvent(ProjectStageEventEntity.builder()
                .projectId(stage.getProjectId()).stageInstanceId(stage.getId()).stageCode(stage.getStageCode())
                .eventType(eventType(request.command())).fromStatus(stage.getStatus()).toStatus(target)
                .activationNumber(activationRound).relatedObjectType(request.relatedObjectType())
                .relatedObjectId(request.relatedObjectId()).relatedObjectVersion(request.relatedObjectVersion())
                .actorType(actor.type()).actorId(actor.actorId()).source(source)
                .authorizationBasis(serializeAuthorizationBasis(authorization.basis()))
                .reason(request.reason()).requestId(request.requestId()).occurredAt(now).build());
    }

    private StageEventType eventType(WorkflowCommandType command) {
        return switch (command) {
            case ACTIVATE -> StageEventType.ACTIVATED;
            case START_PROCESSING -> StageEventType.PROCESSING;
            case WAIT_FOR_CUSTOMER -> StageEventType.WAITING;
            case REQUEST_REVIEW -> StageEventType.REVIEW_REQUESTED;
            case COMPLETE -> StageEventType.COMPLETED;
            case SUSPEND -> StageEventType.SUSPENDED;
            case RESUME -> StageEventType.RESUMED;
            case REOPEN -> StageEventType.REOPENED;
        };
    }

    private int activationRound(ProjectStageInstanceEntity stage, WorkflowCommandType command) {
        return activates(command) ? stage.getActivationCount() + 1 : stage.getActivationCount();
    }

    private boolean activates(WorkflowCommandType command) {
        return command == WorkflowCommandType.ACTIVATE || command == WorkflowCommandType.REOPEN;
    }

    private StageEventSource source(ActorRef actor) {
        return switch (actor.type()) {
            case CUSTOMER_USER -> StageEventSource.CUSTOMER_UI;
            case DESIGNER_USER -> StageEventSource.DESIGNER_UI;
            case ADMIN_USER -> StageEventSource.ADMIN_UI;
            case COORDINATOR_AGENT, STAGE_AGENT -> StageEventSource.AUTOMATION;
            case SYSTEM_EVENT -> StageEventSource.SYSTEM;
        };
    }

    private String serializeAuthorizationBasis(ProjectAuthorizationService.AuthorizationBasis basis) {
        try {
            return objectMapper.writeValueAsString(basis);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize workflow authorization basis", exception);
        }
    }

    private void applyInMemory(ProjectStageInstanceEntity stage, StageStatus target, int activationRound,
                               LocalDateTime activatedAt, LocalDateTime completedAt, LocalDateTime updatedAt) {
        stage.setStatus(target);
        stage.setActivationCount(activationRound);
        if (activatedAt != null) {
            stage.setActivatedAt(activatedAt);
        }
        stage.setCompletedAt(completedAt);
        stage.setVersion(stage.getVersion() + 1);
        stage.setUpdatedAt(updatedAt);
    }

    private ApiException conflict(String message) {
        return new ApiException(ApiErrorCode.CONFLICT.getCode(), message);
    }
}
