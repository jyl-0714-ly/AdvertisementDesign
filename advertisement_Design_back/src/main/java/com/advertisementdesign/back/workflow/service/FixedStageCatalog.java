package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.workflow.entity.ProjectStageInstanceEntity;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageStatus;
import com.advertisementdesign.back.workflow.model.WorkflowModels;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FixedStageCatalog {
    private static final List<WorkflowModels.StageDefinition> DEFINITIONS = List.of(
            definition(StageCode.REQUIREMENT_GUIDE, "需求引导", 1),
            definition(StageCode.CONTRACT_PREPAYMENT, "签订合同预付款", 2),
            definition(StageCode.RESEARCH_REPORT, "资料调研报告", 3),
            definition(StageCode.SKETCH_STYLE, "草图风格敲定", 4),
            definition(StageCode.REVIEW_FINAL, "审稿定稿", 5),
            definition(StageCode.DELIVERY_FINAL_PAYMENT, "交付尾款", 6),
            definition(StageCode.AFTER_SALE_REPURCHASE, "售后复购", 7));

    public List<WorkflowModels.StageDefinition> definitions() {
        return DEFINITIONS;
    }

    public List<ProjectStageInstanceEntity> createInitialInstances(Long projectId, LocalDateTime now) {
        return DEFINITIONS.stream().map(definition -> ProjectStageInstanceEntity.builder()
                .projectId(projectId).stageCode(definition.code()).stageName(definition.name())
                .sortOrder(definition.sortOrder()).status(definition.initialStatus()).activationCount(0)
                .version(0L).createdAt(now).updatedAt(now).build()).toList();
    }

    private static WorkflowModels.StageDefinition definition(StageCode code, String name, int order) {
        return new WorkflowModels.StageDefinition(code, name, order, StageStatus.NOT_STARTED);
    }
}
