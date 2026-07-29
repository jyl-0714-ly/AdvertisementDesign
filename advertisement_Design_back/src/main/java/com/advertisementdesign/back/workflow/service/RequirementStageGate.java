package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.WorkflowCommandType;
import org.springframework.stereotype.Component;

@Component
public class RequirementStageGate implements WorkflowStageGate {
    private final ProjectQueryService projectQueryService;

    public RequirementStageGate(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
    }

    @Override
    public boolean supports(StageCode stageCode) {
        return stageCode == StageCode.REQUIREMENT_GUIDE;
    }

    @Override
    public void verify(Context context) {
        if (context.command() != WorkflowCommandType.COMPLETE) {
            return;
        }
        ProjectModels.ProjectFullDetailView project =
                projectQueryService.requireFullDetail(context.projectId());
        Long confirmedVersionId = project.confirmedRequirementVersionId();
        if (confirmedVersionId == null
                || !"REQUIREMENT_VERSION".equals(context.relatedObjectType())
                || !confirmedVersionId.equals(context.relatedObjectId())
                || context.relatedObjectVersion() == null) {
            throw new ApiException(
                    ApiErrorCode.BUSINESS_ERROR.getCode(),
                    "需求阶段必须绑定已确认的不可变需求版本"
            );
        }
    }
}
