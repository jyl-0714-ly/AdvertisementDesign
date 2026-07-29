package com.advertisementdesign.back.artifact.service;

import com.advertisementdesign.back.artifact.entity.ArtifactConfirmationEntity;
import com.advertisementdesign.back.artifact.entity.ArtifactEntity;
import com.advertisementdesign.back.artifact.entity.ArtifactVersionEntity;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ArtifactStatus;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ArtifactType;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ConfirmationResult;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.PublicationStatus;
import com.advertisementdesign.back.artifact.model.ArtifactModels;
import com.advertisementdesign.back.artifact.repository.ArtifactRepository;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtifactHighRiskTest {
    @Mock private ArtifactRepository repository;
    @Mock private ProjectAuthorizationService authorizationService;
    @Mock private CurrentActorProvider currentActorProvider;
    @Mock private FileService fileService;
    @Mock private ProjectQueryService projectQueryService;

    private ArtifactService artifactService;
    private ArtifactFileService artifactFileService;

    @BeforeEach
    void setUp() {
        artifactService = new ArtifactService(repository, authorizationService, currentActorProvider);
        artifactFileService = new ArtifactFileService(
                fileService, repository, authorizationService, projectQueryService, currentActorProvider);
        org.mockito.Mockito.lenient().when(currentActorProvider.requireCurrentActor()).thenReturn(
                new CurrentActorProvider.CurrentActor(
                        new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 8L), "客户"));
    }

    @Test
    void publishedVersionCannotBeModified() {
        allow(ProjectAuthorizationService.ProjectAction.REVIEW_ARTIFACT);
        when(repository.findArtifact(1L, 2L)).thenReturn(Optional.of(artifact(ArtifactType.REQUIREMENT, 0L)));
        when(repository.findVersion(1L, 2L, 3L)).thenReturn(Optional.of(version(3L, PublicationStatus.PUBLISHED)));

        ApiException exception = assertThrows(ApiException.class, () -> artifactService.updateDraft(
                1L, 2L, 3L, new ArtifactModels.UpdateDraftRequest(0L, 0L, Map.of("text", "changed"), "hash")));

        assertEquals(409, exception.getCode());
        verify(repository, never()).updateDraftVersion(any());
    }

    @Test
    void confirmationBindsSpecificImmutableVersion() {
        allow(ProjectAuthorizationService.ProjectAction.CONFIRM_REQUIREMENT);
        when(repository.findArtifact(1L, 2L)).thenReturn(Optional.of(artifact(ArtifactType.REQUIREMENT, 0L)));
        when(repository.findVersion(1L, 2L, 3L)).thenReturn(Optional.of(version(3L, PublicationStatus.PUBLISHED)));
        when(repository.findConfirmation(1L, "confirm-1")).thenReturn(Optional.empty());
        when(repository.confirmOnce(any(), eq(0L), any())).thenAnswer(invocation -> {
            ArtifactConfirmationEntity confirmation = invocation.getArgument(2);
            confirmation.setId(20L);
            return true;
        });

        ArtifactModels.ConfirmationView result = artifactService.confirm(1L, 2L, 3L,
                new ArtifactModels.ConfirmRequest(0L, ConfirmationResult.CONFIRMED, "confirm-1", null));

        ArgumentCaptor<ArtifactConfirmationEntity> captor = ArgumentCaptor.forClass(ArtifactConfirmationEntity.class);
        verify(repository).confirmOnce(any(), eq(0L), captor.capture());
        assertEquals(3L, captor.getValue().getArtifactVersionId());
        assertEquals(1, captor.getValue().getArtifactVersionNumber());
        assertEquals(3L, result.artifactVersionId());
    }

    @Test
    void unauthorizedMemberCannotConfirm() {
        when(repository.findArtifact(1L, 2L)).thenReturn(Optional.of(artifact(ArtifactType.REQUIREMENT, 0L)));
        when(repository.findVersion(1L, 2L, 3L)).thenReturn(Optional.of(version(3L, PublicationStatus.PUBLISHED)));
        when(authorizationService.authorize(1L, ProjectAuthorizationService.ProjectAction.CONFIRM_REQUIREMENT))
                .thenReturn(new ProjectAuthorizationService.AuthorizationDecision(false,
                        ProjectAuthorizationService.AccessLevel.FULL, basis()));

        ApiException exception = assertThrows(ApiException.class, () -> artifactService.confirm(1L, 2L, 3L,
                new ArtifactModels.ConfirmRequest(0L, ConfirmationResult.CONFIRMED, "confirm-2", null)));

        assertEquals(403, exception.getCode());
        verify(repository, never()).confirmOnce(any(), any(), any());
    }

    @Test
    void concurrentConfirmationSucceedsOnlyOnce() {
        allow(ProjectAuthorizationService.ProjectAction.CONFIRM_REQUIREMENT);
        when(repository.findArtifact(1L, 2L)).thenReturn(Optional.of(artifact(ArtifactType.REQUIREMENT, 0L)));
        when(repository.findVersion(1L, 2L, 3L)).thenReturn(Optional.of(version(3L, PublicationStatus.PUBLISHED)));
        when(repository.findConfirmation(eq(1L), any())).thenReturn(Optional.empty());
        when(repository.confirmOnce(any(), eq(0L), any())).thenReturn(true, false);

        artifactService.confirm(1L, 2L, 3L,
                new ArtifactModels.ConfirmRequest(0L, ConfirmationResult.CONFIRMED, "race-a", null));
        ApiException loser = assertThrows(ApiException.class, () -> artifactService.confirm(1L, 2L, 3L,
                new ArtifactModels.ConfirmRequest(0L, ConfirmationResult.CONFIRMED, "race-b", null)));

        assertEquals(409, loser.getCode());
        verify(repository, times(2)).confirmOnce(any(), eq(0L), any());
    }

    @Test
    void crossProjectDownloadIsRejected() {
        allow(ProjectAuthorizationService.ProjectAction.VIEW_FULL);
        when(repository.findVersion(1L, 2L, 3L)).thenReturn(Optional.of(version(3L, PublicationStatus.PUBLISHED)));
        when(repository.fileBelongsToVersion(1L, 3L, 9L)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> artifactFileService.openDownload(1L, 2L, 3L, 9L));

        assertEquals(403, exception.getCode());
        verify(fileService, never()).openStream(any());
    }

    @Test
    void unreviewedSketchIsNotVisible() {
        allow(ProjectAuthorizationService.ProjectAction.VIEW_FULL);
        when(repository.findArtifact(1L, 2L)).thenReturn(Optional.of(artifact(ArtifactType.SKETCH, 0L)));
        when(repository.findVersion(1L, 2L, 3L)).thenReturn(Optional.of(version(3L, PublicationStatus.PUBLISHED)));
        when(repository.hasApproval(1L, 3L)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> artifactService.customerVersion(1L, 2L, 3L));

        assertEquals(404, exception.getCode());
        verify(repository).hasApproval(1L, 3L);
    }

    private void allow(ProjectAuthorizationService.ProjectAction action) {
        when(authorizationService.authorize(1L, action)).thenReturn(
                new ProjectAuthorizationService.AuthorizationDecision(
                        true, ProjectAuthorizationService.AccessLevel.FULL, basis()));
    }

    private ProjectAuthorizationService.AuthorizationBasis basis() {
        return new ProjectAuthorizationService.AuthorizationBasis("PROJECT_MEMBER", 30L, Set.of("CONFIRM"));
    }

    private ArtifactEntity artifact(ArtifactType type, Long objectVersion) {
        return ArtifactEntity.builder().id(2L).projectId(1L).artifactType(type).title("产物")
                .status(ArtifactStatus.PUBLISHED).latestVersionNumber(1).version(objectVersion).build();
    }

    private ArtifactVersionEntity version(Long id, PublicationStatus status) {
        return ArtifactVersionEntity.builder().id(id).artifactId(2L).projectId(1L).versionNumber(1)
                .publicationStatus(status).generated(false).version(0L).build();
    }
}
