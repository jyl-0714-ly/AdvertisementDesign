package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.advertisementdesign.back.workflow.converter.WorkflowConverter;
import com.advertisementdesign.back.workflow.model.WorkflowModels;
import com.advertisementdesign.back.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
