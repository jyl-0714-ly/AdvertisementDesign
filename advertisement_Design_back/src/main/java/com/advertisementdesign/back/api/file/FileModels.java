package com.advertisementdesign.back.api.file;

import com.advertisementdesign.back.domain.enums.FileRole;
import com.advertisementdesign.back.domain.enums.FileStatus;
import com.advertisementdesign.back.domain.enums.StorageProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "文件相关模型")
public final class FileModels {
    private FileModels() {
    }

    @Schema(description = "上传文件响应")
    public record UploadFileResponse(
            Long id,
            String originalName,
            String url,
            String mimeType,
            Long fileSize,
            FileStatus status
    ) {
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
            FileAssetVO file,
            String createdAt
    ) {
    }

    @Schema(description = "文件视图")
    public record FileAssetVO(
            Long id,
            String originalName,
            String storageName,
            StorageProvider storageProvider,
            String bucketName,
            String objectKey,
            String url,
            String mimeType,
            Long fileSize,
            String fileHash,
            FileStatus status,
            String createdAt,
            String updatedAt
    ) {
    }
}
