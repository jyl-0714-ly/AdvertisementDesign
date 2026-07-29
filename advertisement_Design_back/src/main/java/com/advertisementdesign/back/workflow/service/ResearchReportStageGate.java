package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ArtifactType;
import com.advertisementdesign.back.artifact.service.ArtifactService;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.WorkflowCommandType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResearchReportStageGate implements WorkflowStageGate {
    private final ArtifactService artifactService;

    @Override
    public boolean supports(StageCode stageCode) {
        return stageCode == StageCode.RESEARCH_REPORT;
    }

    @Override
    public void verify(Context context) {
        if (context.command() != WorkflowCommandType.COMPLETE) return;
        if (!"ARTIFACT_VERSION".equals(context.relatedObjectType())
                || context.relatedObjectId() == null || context.relatedObjectVersion() == null
                || !artifactService.hasConfirmedVersion(context.projectId(), ArtifactType.RESEARCH_REPORT,
                context.relatedObjectId(), Math.toIntExact(context.relatedObjectVersion()))) {
            throw new ApiException(ApiErrorCode.BUSINESS_ERROR.getCode(), "调研阶段必须绑定客户已确认的报告版本");
        }
    }
}
