package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.advertisementdesign.back.workflow.converter.WorkflowConverter;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageStatus;
import com.advertisementdesign.back.workflow.model.WorkflowModels;
import com.advertisementdesign.back.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkflowQueryService {
    private final WorkflowRepository repository;
    private final WorkflowConverter converter;
    private final ProjectQueryService projectQueryService;
    private final ProjectAuthorizationService authorizationService;

    public List<WorkflowModels.StageInstanceView> stages(Long projectId) {
        requireProject(projectId);
        requireFullAccess(projectId);
        return repository.findStages(projectId).stream().map(converter::toStage).toList();
    }

    public WorkflowModels.CurrentStageWorkspaceView currentStage(Long projectId) {
        requireProject(projectId);
        requireFullAccess(projectId);
        WorkflowModels.StageInstanceView current = repository.findStages(projectId).stream()
                .map(converter::toStage)
                .filter(stage -> stage.status() != StageStatus.NOT_STARTED && stage.status() != StageStatus.COMPLETED)
                .findFirst()
                .orElseGet(() -> repository.findStages(projectId).stream().map(converter::toStage)
                        .filter(stage -> stage.status() == StageStatus.COMPLETED)
                        .reduce((first, second) -> second)
                        .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND)));
        return new WorkflowModels.CurrentStageWorkspaceView(
                current, allowedActions(projectId, current.stageCode(), current.status()), List.of(), List.of());
    }

    private Set<String> allowedActions(Long projectId, StageCode stageCode, StageStatus status) {
        if (status == StageStatus.NOT_STARTED) return Set.of();
        boolean canUpdate = authorizationService.authorize(
                projectId, ProjectAuthorizationService.ProjectAction.UPDATE_PROJECT).allowed();
        if (status == StageStatus.SUSPENDED) {
            return canUpdate ? Set.of("RESUME") : Set.of();
        }
        if (status == StageStatus.COMPLETED) {
            return canUpdate ? Set.of("REOPEN") : Set.of();
        }

        ProjectAuthorizationService.ProjectAction completionAction = switch (stageCode) {
            case REQUIREMENT_GUIDE -> ProjectAuthorizationService.ProjectAction.CONFIRM_REQUIREMENT;
            case CONTRACT_PREPAYMENT -> ProjectAuthorizationService.ProjectAction.MANAGE_PAYMENT;
            case RESEARCH_REPORT -> ProjectAuthorizationService.ProjectAction.CONFIRM_REPORT;
            case SKETCH_STYLE, REVIEW_FINAL -> ProjectAuthorizationService.ProjectAction.CONFIRM_DESIGN;
            case DELIVERY_FINAL_PAYMENT -> ProjectAuthorizationService.ProjectAction.RECEIVE_DELIVERY;
            case AFTER_SALE_REPURCHASE -> ProjectAuthorizationService.ProjectAction.UPDATE_PROJECT;
        };
        boolean canComplete = authorizationService.authorize(projectId, completionAction).allowed();
        if (stageCode == StageCode.REQUIREMENT_GUIDE) {
            canComplete = canComplete && projectQueryService.requireFullDetail(projectId)
                    .confirmedRequirementVersionId() != null;
        } else if (stageCode != StageCode.AFTER_SALE_REPURCHASE) {
            canComplete = false;
        }

        java.util.LinkedHashSet<String> actions = new java.util.LinkedHashSet<>();
        if (canUpdate) {
            actions.add("START_PROCESSING");
            actions.add("WAIT_FOR_CUSTOMER");
            actions.add("REQUEST_REVIEW");
            actions.add("SUSPEND");
        }
        if (canComplete) actions.add("COMPLETE");
        return Set.copyOf(actions);
    }

    public List<WorkflowModels.StageEventView> history(Long projectId, Long stageInstanceId) {
        requireProject(projectId);
        requireFullAccess(projectId);
        repository.findStage(projectId, stageInstanceId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        return repository.findEvents(projectId, stageInstanceId).stream().map(converter::toEvent).toList();
    }

    private void requireProject(Long projectId) {
        if (projectQueryService.findContext(projectId).isEmpty()) throw new ApiException(ApiErrorCode.NOT_FOUND);
    }

    private void requireFullAccess(Long projectId) {
        if (!authorizationService.authorize(projectId, ProjectAuthorizationService.ProjectAction.VIEW_FULL).allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
