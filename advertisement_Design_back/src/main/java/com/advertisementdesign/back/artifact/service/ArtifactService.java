package com.advertisementdesign.back.artifact.service;

import com.advertisementdesign.back.artifact.entity.*;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.*;
import com.advertisementdesign.back.artifact.model.ArtifactModels;
import com.advertisementdesign.back.artifact.repository.ArtifactRepository;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ArtifactService {
    private final ArtifactRepository repository;
    private final ProjectAuthorizationService authorizationService;
    private final CurrentActorProvider currentActorProvider;

    @Transactional
    public ArtifactModels.ArtifactView create(Long projectId, ArtifactModels.CreateArtifactRequest request) {
        require(ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT, projectId);
        ActorRef actor = actor();
        LocalDateTime now = LocalDateTime.now();
        ArtifactEntity artifact = repository.insert(ArtifactEntity.builder().projectId(projectId)
                .stageInstanceId(request.stageInstanceId()).artifactType(request.artifactType()).title(request.title())
                .status(ArtifactStatus.DRAFT).latestVersionNumber(0).version(0L)
                .createdByActorType(actor.type()).createdByActorId(actor.actorId()).createdAt(now).updatedAt(now).build());
        return artifactView(artifact);
    }

    @Transactional
    public ArtifactModels.VersionView createVersion(Long projectId, Long artifactId, ArtifactModels.CreateVersionRequest request) {
        require(ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT, projectId);
        ArtifactEntity artifact = artifact(projectId, artifactId);
        int next = artifact.getLatestVersionNumber() + 1;
        if (next > 1 && request.parentVersionId() == null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "修订版本必须指定父版本");
        }
        if (next == 1 && request.parentVersionId() != null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "首个版本不能指定父版本");
        }
        if (request.parentVersionId() != null) {
            ArtifactVersionEntity parent = repository.findVersion(projectId, artifactId, request.parentVersionId())
                    .orElseThrow(() -> new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "父版本不存在"));
            if (!Objects.equals(parent.getVersionNumber(), next - 1)) {
                throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "父版本必须是当前最新版本");
            }
        }
        ActorRef actor = actor();
        ArtifactVersionEntity version = repository.insertVersion(ArtifactVersionEntity.builder()
                .artifactId(artifactId).projectId(projectId).versionNumber(next).parentVersionId(request.parentVersionId())
                .content(request.content()).contentHash(request.contentHash()).publicationStatus(PublicationStatus.DRAFT)
                .generated(request.generated())
                .createdByActorType(actor.type()).createdByActorId(actor.actorId()).version(0L).createdAt(LocalDateTime.now()).build());
        artifact.setLatestVersionNumber(next);
        artifact.setUpdatedAt(LocalDateTime.now());
        repository.updateArtifact(artifact);
        return versionView(artifact, version);
    }

    @Transactional
    public ArtifactModels.VersionView updateDraft(Long projectId, Long artifactId, Long versionId,
                                                   ArtifactModels.UpdateDraftRequest request) {
        require(ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT, projectId);
        ArtifactEntity artifact = artifact(projectId, artifactId);
        ArtifactVersionEntity version = version(projectId, artifactId, versionId);
        if (version.getPublicationStatus() != PublicationStatus.DRAFT || !repository.confirmed(projectId, versionId).isEmpty()) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "已发布或已确认版本不可原地修改");
        }
        if (!Objects.equals(artifact.getVersion(), request.objectVersion())
                || !Objects.equals(version.getVersion(), request.versionObjectVersion())) throw conflict();
        version.setContent(request.content());
        version.setContentHash(request.contentHash());
        repository.updateDraftVersion(version);
        return versionView(artifact, version);
    }

    @Transactional
    public ArtifactModels.VersionView approve(Long projectId, Long artifactId, Long versionId,
                                               ArtifactModels.ApproveRequest request) {
        ProjectAuthorizationService.AuthorizationDecision decision = require(ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT, projectId);
        ArtifactEntity artifact = artifact(projectId, artifactId);
        ArtifactVersionEntity version = version(projectId, artifactId, versionId);
        requireObjectVersion(artifact, request.objectVersion());
        repository.insertApproval(ArtifactApprovalEntity.builder().projectId(projectId).artifactVersionId(versionId)
                .decision(request.decision()).reviewerUserId(actor().actorId())
                .assignmentId(decision.basis().relationshipId()).comment(request.comment()).requestId(request.requestId())
                .decidedAt(LocalDateTime.now()).build());
        artifact.setStatus(request.decision() == ApprovalDecision.APPROVED ? ArtifactStatus.UNDER_REVIEW : ArtifactStatus.DRAFT);
        artifact.setUpdatedAt(LocalDateTime.now());
        repository.updateArtifact(artifact);
        return versionView(artifact, version);
    }

    @Transactional
    public ArtifactModels.VersionView publish(Long projectId, Long artifactId, Long versionId,
                                               ArtifactModels.PublishRequest request) {
        require(ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT, projectId);
        ArtifactEntity artifact = artifact(projectId, artifactId);
        ArtifactVersionEntity version = version(projectId, artifactId, versionId);
        requireObjectVersion(artifact, request.objectVersion());
        if (!Objects.equals(version.getVersion(), request.versionObjectVersion())) throw conflict();
        if (version.getPublicationStatus() != PublicationStatus.DRAFT) throw conflict();
        if (requiresApproval(artifact.getArtifactType()) && !repository.hasApproval(projectId, versionId)) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "草图和正式设计发布前必须完成真实审核");
        }
        version.setPublicationStatus(PublicationStatus.PUBLISHED);
        version.setPublishedByUserId(actor().actorId());
        version.setPublishedAt(LocalDateTime.now());
        repository.updateDraftVersion(version);
        artifact.setStatus(ArtifactStatus.PUBLISHED);
        artifact.setUpdatedAt(LocalDateTime.now());
        repository.updateArtifact(artifact);
        return versionView(artifact, version);
    }

    @Transactional
    public ArtifactModels.ConfirmationView confirm(Long projectId, Long artifactId, Long versionId,
                                                   ArtifactModels.ConfirmRequest request) {
        ArtifactEntity artifact = artifact(projectId, artifactId);
        ArtifactVersionEntity version = version(projectId, artifactId, versionId);
        ProjectAuthorizationService.AuthorizationDecision decision = require(confirmAction(artifact.getArtifactType()), projectId);
        requireObjectVersion(artifact, request.objectVersion());
        if (version.getPublicationStatus() != PublicationStatus.PUBLISHED) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "只能确认已发布的不可变版本");
        }
        var existing = repository.findConfirmation(projectId, request.idempotencyKey());
        if (existing.isPresent()) {
            ArtifactConfirmationEntity confirmation = existing.get();
            if (!Objects.equals(confirmation.getArtifactVersionId(), versionId)) throw conflict();
            return confirmationView(confirmation);
        }
        ActorRef actor = actor();
        ArtifactConfirmationEntity confirmation = ArtifactConfirmationEntity.builder()
                .projectId(projectId).artifactId(artifactId).artifactVersionId(versionId)
                .artifactVersionNumber(version.getVersionNumber()).confirmationType(confirmationType(artifact.getArtifactType()))
                .result(request.result()).actorType(actor.type()).actorId(actor.actorId())
                .customerMemberId(decision.basis().relationshipId()).authorizationBasis(basis(decision.basis()))
                .objectVersion(request.objectVersion()).comment(request.comment()).idempotencyKey(request.idempotencyKey())
                .confirmedAt(LocalDateTime.now()).build();
        if (!repository.confirmOnce(artifact, request.objectVersion(), confirmation)) throw conflict();
        return confirmationView(confirmation);
    }

    @Transactional
    public ArtifactModels.AnnotationView annotate(Long projectId, Long artifactId, Long versionId,
                                                   ArtifactModels.AnnotationRequest request) {
        require(ProjectAuthorizationService.ProjectAction.VIEW_FULL, projectId);
        ArtifactVersionEntity version = version(projectId, artifactId, versionId);
        if (version.getPublicationStatus() != PublicationStatus.DRAFT) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "已发布版本不可追加标注");
        }
        if (!repository.confirmed(projectId, versionId).isEmpty()) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "已确认版本不可追加标注");
        }
        if (!repository.fileBelongsToVersion(projectId, versionId, request.fileId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "标注文件不属于当前产物版本");
        }
        ActorRef actor = actor();
        ArtifactAnnotationEntity annotation = repository.insertAnnotation(ArtifactAnnotationEntity.builder()
                .projectId(projectId).artifactVersionId(versionId).fileAssetId(request.fileId())
                .annotationType(request.annotationType()).geometry(request.geometry()).content(request.content())
                .actorType(actor.type()).actorId(actor.actorId()).createdAt(LocalDateTime.now()).build());
        return new ArtifactModels.AnnotationView(annotation.getId(), annotation.getProjectId(),
                annotation.getArtifactVersionId(), annotation.getFileAssetId(), annotation.getAnnotationType(),
                annotation.getGeometry(), annotation.getContent(), annotation.getActorId(),
                annotation.getCreatedAt().toString());
    }

    public ArtifactModels.VersionView customerVersion(Long projectId, Long artifactId, Long versionId) {
        require(ProjectAuthorizationService.ProjectAction.VIEW_FULL, projectId);
        ArtifactEntity artifact = artifact(projectId, artifactId);
        ArtifactVersionEntity version = version(projectId, artifactId, versionId);
        if (version.getPublicationStatus() != PublicationStatus.PUBLISHED
                || requiresApproval(artifact.getArtifactType()) && !repository.hasApproval(projectId, versionId)) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return versionView(artifact, version);
    }

    public boolean hasConfirmedVersion(Long projectId, ArtifactType type, Long versionId, Integer versionNumber) {
        return repository.findConfirmedVersion(projectId, type, versionId)
                .filter(confirmed -> Objects.equals(confirmed.versionNumber(), versionNumber)).isPresent();
    }

    public boolean hasConfirmedVersion(Long projectId, ArtifactType type) {
        return repository.findLatestConfirmedVersion(projectId, type).isPresent();
    }

    private ArtifactEntity artifact(Long projectId, Long artifactId) { return repository.findArtifact(projectId, artifactId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND)); }
    private ArtifactVersionEntity version(Long p, Long a, Long v) { return repository.findVersion(p, a, v).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND)); }
    private ActorRef actor() { return currentActorProvider.requireCurrentActor().actor(); }
    private ProjectAuthorizationService.AuthorizationDecision require(ProjectAuthorizationService.ProjectAction action, Long projectId) {
        var decision = authorizationService.authorize(projectId, action);
        if (!decision.allowed()) throw new ApiException(ApiErrorCode.FORBIDDEN);
        return decision;
    }
    private void requireObjectVersion(ArtifactEntity artifact, Long version) { if (!Objects.equals(artifact.getVersion(), version)) throw conflict(); }
    private ApiException conflict() { return new ApiException(ApiErrorCode.CONFLICT.getCode(), "产物状态已变化，请刷新后重试"); }
    private boolean requiresApproval(ArtifactType type) { return type == ArtifactType.SKETCH || type == ArtifactType.FORMAL_DESIGN; }
    private ProjectAuthorizationService.ProjectAction confirmAction(ArtifactType type) { return switch (type) {
        case REQUIREMENT -> ProjectAuthorizationService.ProjectAction.CONFIRM_REQUIREMENT;
        case RESEARCH_REPORT -> ProjectAuthorizationService.ProjectAction.CONFIRM_REPORT;
        case SKETCH, FORMAL_DESIGN -> ProjectAuthorizationService.ProjectAction.CONFIRM_DESIGN;
        case DELIVERY -> ProjectAuthorizationService.ProjectAction.RECEIVE_DELIVERY;
    }; }
    private ConfirmationType confirmationType(ArtifactType type) { return switch (type) {
        case REQUIREMENT -> ConfirmationType.REQUIREMENT; case RESEARCH_REPORT -> ConfirmationType.REPORT;
        case SKETCH -> ConfirmationType.SKETCH; case FORMAL_DESIGN -> ConfirmationType.FORMAL_DESIGN;
        case DELIVERY -> ConfirmationType.DELIVERY_RECEIPT;
    }; }
    private Map<String,Object> basis(ProjectAuthorizationService.AuthorizationBasis b) {
        Map<String,Object> value = new LinkedHashMap<>(); value.put("source", b.source()); value.put("relationshipId", b.relationshipId());
        value.put("relationshipVersion", b.relationshipVersion()); value.put("scopes", b.scopes()); return value;
    }
    private ArtifactModels.ArtifactView artifactView(ArtifactEntity a) { return new ArtifactModels.ArtifactView(a.getId(), a.getProjectId(), a.getArtifactType(), a.getTitle(), a.getStatus(), a.getLatestVersionNumber(), a.getVersion()); }
    private ArtifactModels.VersionView versionView(ArtifactEntity a, ArtifactVersionEntity v) { return new ArtifactModels.VersionView(v.getId(), v.getArtifactId(), v.getProjectId(), v.getVersionNumber(), v.getParentVersionId(), v.getPublicationStatus(), v.getPublicationStatus() == PublicationStatus.PUBLISHED && (!requiresApproval(a.getArtifactType()) || repository.hasApproval(v.getProjectId(), v.getId())), v.getContent(), v.getContentHash()); }
    private ArtifactModels.ConfirmationView confirmationView(ArtifactConfirmationEntity c) { return new ArtifactModels.ConfirmationView(c.getId(), c.getProjectId(), c.getArtifactId(), c.getArtifactVersionId(), c.getArtifactVersionNumber(), c.getConfirmationType(), c.getResult(), c.getActorId(), c.getObjectVersion(), c.getIdempotencyKey()); }
}
