package com.advertisementdesign.back.project.model;

import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.project.enums.FileRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "项目文件相关模型")
public final class ProjectFileModels {
    private ProjectFileModels() {
    }

    @Schema(description = "归档项目文件请求")
    public record CreateProjectFileRequest(
            @NotNull Long fileId,
            Long projectStageId,
            String stageCode,
            FileRole fileRole,
            String description
    ) {
    }

    @Schema(description = "项目文件视图")
    public record ProjectFileVO(
            Long id,
            Long projectId,
            Long projectStageId,
            String stageCode,
            Long fileId,
            Long uploaderId,
            FileRole fileRole,
            String description,
            FileModels.FileAssetVO file,
            String createdAt
    ) {
    }
}
