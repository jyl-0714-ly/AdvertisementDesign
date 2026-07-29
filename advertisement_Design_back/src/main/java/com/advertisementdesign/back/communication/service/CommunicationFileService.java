package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommunicationFileService {
    private final FileService fileService;
    private final CommunicationRepository relationRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectQueryService projectQueryService;
    private final CurrentActorProvider currentActorProvider;

    @Transactional
    public FileModels.CustomerSafeFileMetadata uploadDraft(Long projectId, MultipartFile file) {
        ProjectModels.ProjectContextView project = projectQueryService.findContext(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        requireAllowed(projectId, ProjectAuthorizationService.ProjectAction.SEND_MESSAGE);
        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        FileModels.FileAssetVO uploaded = fileService.upload(file, StorageScene.CONVERSATION_ATTACHMENT, uploader(actor));
        FileService.AssetMetadata asset = fileService.assignOrganization(uploaded.id(), project.organizationId());
        return metadata(asset, projectId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void claimDrafts(Long projectId, Long organizationId, ActorRef actor, List<Long> fileAssetIds) {
        List<Long> ids = fileAssetIds == null ? List.of() : fileAssetIds.stream().distinct().toList();
        if (ids.size() > 10) throw new ApiException(400, "附件数量超过限制");
        for (Long id : ids) {
            fileService.claimProjectMessageDraft(id, uploader(actor), organizationId, projectId);
        }
    }

    public Download openDownload(Long projectId, Long fileId) {
        requireAllowed(projectId, ProjectAuthorizationService.ProjectAction.VIEW_FULL);
        if (!relationRepository.isAttachedToProject(projectId, fileId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "文件不属于当前项目会话");
        }
        FileService.AssetMetadata asset = fileService.requireActiveMetadata(fileId);
        if (!Objects.equals(asset.projectId(), projectId)
                || asset.businessScope() != FileBusinessScope.PROJECT_COMMUNICATION) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return new Download(asset.originalName(), asset.mimeType(), asset.fileSize(), fileService.openStream(fileId));
    }

    public FileModels.CustomerSafeFileMetadata metadataForAttached(Long projectId, Long fileId) {
        if (!relationRepository.isAttachedToProject(projectId, fileId)) throw new ApiException(ApiErrorCode.FORBIDDEN);
        return metadata(fileService.requireActiveMetadata(fileId), projectId);
    }

    private FileModels.CustomerSafeFileMetadata metadata(FileService.AssetMetadata asset, Long projectId) {
        return new FileModels.CustomerSafeFileMetadata(asset.id(), asset.originalName(), asset.mimeType(),
                asset.fileSize(), "/api/projects/" + projectId + "/conversation/files/" + asset.id() + "/download");
    }

    private FileService.Uploader uploader(ActorRef actor) {
        return new FileService.Uploader(actor.type().name(), actor.actorId());
    }

    private void requireAllowed(Long projectId, ProjectAuthorizationService.ProjectAction action) {
        if (!authorizationService.authorize(projectId, action).allowed()) throw new ApiException(ApiErrorCode.FORBIDDEN);
    }

    public record Download(String name, String mimeType, Long size, InputStream stream) {
        public InputStreamResource resource() { return new InputStreamResource(stream); }
    }
}
