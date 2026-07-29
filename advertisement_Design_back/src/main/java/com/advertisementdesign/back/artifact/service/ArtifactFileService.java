package com.advertisementdesign.back.artifact.service;

import com.advertisementdesign.back.artifact.entity.ArtifactVersionFileEntity;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.FileRole;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.PublicationStatus;
import com.advertisementdesign.back.artifact.repository.ArtifactRepository;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.enums.StorageZone;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ArtifactFileService {
    private final FileService fileService;
    private final ArtifactRepository artifactRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectQueryService projectQueryService;
    private final CurrentActorProvider currentActorProvider;

    @Transactional
    public FileModels.FileAssetVO uploadDraft(Long projectId, MultipartFile file) {
        requireAllowed(projectId, ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT);
        ProjectModels.ProjectContextView project = projectQueryService.findContext(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        FileModels.FileAssetVO uploaded = fileService.upload(file, StorageScene.PROJECT_OTHER, uploader(actor));
        fileService.assignOrganization(uploaded.id(), project.organizationId());
        return uploaded;
    }

    @Transactional
    public void attach(Long projectId, Long artifactId, Long versionId, Long fileId,
                       FileRole role, Integer displayOrder) {
        requireAllowed(projectId, ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT);
        var version = artifactRepository.findVersion(projectId, artifactId, versionId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (version.getPublicationStatus() != PublicationStatus.DRAFT
                || !artifactRepository.confirmed(projectId, versionId).isEmpty()) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "已发布或已确认版本不可追加文件");
        }
        ProjectModels.ProjectContextView project = projectQueryService.findContext(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        fileService.claimProjectArtifactDraft(fileId, uploader(actor), project.organizationId(), projectId);
        artifactRepository.insertFile(ArtifactVersionFileEntity.builder()
                .artifactVersionId(versionId).fileAssetId(fileId).fileRole(role)
                .displayOrder(displayOrder == null ? 0 : displayOrder).createdAt(LocalDateTime.now()).build());
    }

    public Download openDownload(Long projectId, Long artifactId, Long versionId, Long fileId) {
        requireAllowed(projectId, ProjectAuthorizationService.ProjectAction.VIEW_FULL);
        artifactRepository.findVersion(projectId, artifactId, versionId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!artifactRepository.fileBelongsToVersion(projectId, versionId, fileId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "文件不属于当前项目产物版本");
        }
        FileService.AssetMetadata asset = fileService.requireActiveMetadata(fileId);
        if (!Objects.equals(asset.projectId(), projectId)
                || asset.businessScope() != FileBusinessScope.PROJECT_ARTIFACT
                || asset.storageZone() != StorageZone.PRIVATE) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return new Download(asset.originalName(), asset.mimeType(), asset.fileSize(), fileService.openStream(fileId));
    }

    private FileService.Uploader uploader(ActorRef actor) {
        return new FileService.Uploader(actor.type().name(), actor.actorId());
    }

    private void requireAllowed(Long projectId, ProjectAuthorizationService.ProjectAction action) {
        if (!authorizationService.authorize(projectId, action).allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    public record Download(String name, String mimeType, Long size, InputStream stream) {
        public InputStreamResource resource() { return new InputStreamResource(stream); }
    }
}
