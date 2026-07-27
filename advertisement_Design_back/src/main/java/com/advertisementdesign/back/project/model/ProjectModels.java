package com.advertisementdesign.back.project.model;

import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "项目相关模型")
public final class ProjectModels {
    private ProjectModels() {
    }

    @Schema(description = "创建项目请求")
    public record CreateProjectRequest(@NotNull Long customerId, @NotNull Long designerId, @NotBlank String name, String description) {
    }

    @Schema(description = "更新项目请求")
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
