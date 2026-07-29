package com.advertisementdesign.back.artifact.model;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class ArtifactModels {
    private ArtifactModels() {}

    public record CreateArtifactRequest(@NotNull ArtifactType artifactType, @NotBlank String title, Long stageInstanceId) {}
    public record CreateVersionRequest(Long parentVersionId, Map<String, Object> content, String contentHash, boolean generated) {}
    public record UpdateDraftRequest(@NotNull Long objectVersion, @NotNull Long versionObjectVersion,
                                     Map<String, Object> content, String contentHash) {}
    public record PublishRequest(@NotNull Long objectVersion, @NotNull Long versionObjectVersion) {}
    public record ApproveRequest(@NotNull Long objectVersion, @NotNull ApprovalDecision decision, String comment, String requestId) {}
    public record ConfirmRequest(@NotNull Long objectVersion, @NotNull ConfirmationResult result,
                                 @NotBlank String idempotencyKey, String comment) {}
    public record AttachFileRequest(@NotNull Long fileId, @NotNull FileRole fileRole, Integer displayOrder) {}
    public record AnnotationRequest(@NotNull Long fileId, @NotNull AnnotationType annotationType,
                                    @NotNull Map<String, Object> geometry, String content) {}
    public record AnnotationView(Long id, Long projectId, Long artifactVersionId, Long fileId,
                                 AnnotationType annotationType, Map<String, Object> geometry,
                                 String content, Long actorId, String createdAt) {}
    public record ArtifactView(Long id, Long projectId, ArtifactType artifactType, String title,
                               ArtifactStatus status, Integer latestVersionNumber, Long objectVersion) {}
    public record VersionView(Long id, Long artifactId, Long projectId, Integer versionNumber,
                              Long parentVersionId, PublicationStatus publicationStatus, boolean customerVisible,
                              Map<String, Object> content, String contentHash) {}
    public record ConfirmationView(Long id, Long projectId, Long artifactId, Long artifactVersionId,
                                   Integer artifactVersionNumber, ConfirmationType confirmationType,
                                   ConfirmationResult result, Long actorId, Long objectVersion, String idempotencyKey) {}
}
