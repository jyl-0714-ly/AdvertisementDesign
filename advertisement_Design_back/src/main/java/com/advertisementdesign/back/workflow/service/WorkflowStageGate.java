package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.workflow.entity.ProjectStageInstanceEntity;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.WorkflowCommandType;

/** Stage-specific business gates. Later commercial and artifact modules plug in here without writing stage state. */
public interface WorkflowStageGate {
    void verify(Context context);

    record Context(Long projectId, ProjectStageInstanceEntity stage, WorkflowCommandType command,
                   String relatedObjectType, Long relatedObjectId, Integer relatedObjectVersion) {
    }

    default boolean supports(StageCode stageCode) {
        return false;
    }
}
