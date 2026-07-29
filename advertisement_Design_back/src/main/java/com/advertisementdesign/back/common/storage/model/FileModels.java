package com.advertisementdesign.back.common.storage.model;

import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "客户安全文件元数据")
    public record CustomerSafeFileMetadata(
            Long id,
            String name,
            String mimeType,
            Long size,
            String downloadPath
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
