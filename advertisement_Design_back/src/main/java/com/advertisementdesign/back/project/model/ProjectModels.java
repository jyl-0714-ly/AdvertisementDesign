package com.advertisementdesign.back.project.model;

import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "项目相关模型")
public final class ProjectModels {
    private ProjectModels() {
    }

    @Schema(description = "更新项目请求；designerId 仅为旧客户端兼容字段，传入将被拒绝")
    public record UpdateProjectRequest(Long designerId, String name, String description, ProjectStatus status) {
    }

    @Schema(description = "项目视图")
    public record ProjectVO(
            Long id,
            String name,
            String description,
            Long customerId,
            String customerName,
            Long designerId,
            String designerName,
            String currentStage,
            String currentStageName,
            ProjectStatus status,
            Integer progress,
            String createdAt,
            String updatedAt
    ) {
    }

    @Schema(description = "项目阶段视图")
    public record ProjectStageVO(
            Long id,
            Long projectId,
            String stageCode,
            String stageName,
            Integer sortOrder,
            ProjectStageStatus status,
            String reachedAt,
            String updatedAt
    ) {
    }
}
