package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.storage.converter.FileConverter;
import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.communication.service.ConversationAccessService;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock
    private StorageRepository storageRepository;
    @Mock
    private ConversationAccessService conversationAccessService;
    @Mock
    private ProjectAuthorizationService projectAuthorizationService;
    @Mock
    private ProjectQueryService projectQueryService;
    @Mock
    private LocalFileStorage localFileStorage;
    @Mock
    private FileConverter converter;
    @Mock
    private CurrentUserProfileProvider authService;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(
                storageRepository, conversationAccessService, projectAuthorizationService, projectQueryService,
                localFileStorage, converter, authService);
    }

    @Test
    void projectMessageDraftRejectsAttachmentUploadedByAnotherUser() {
        FileAssetEntity asset = FileAssetEntity.builder()
                .id(9L).uploaderActorType(ActorRef.ActorType.CUSTOMER_USER).uploaderActorId(99L)
                .organizationId(20L).businessScope(FileBusinessScope.PRIVATE_DRAFT)
                .status(FileStatus.ACTIVE).version(0L).build();
        when(storageRepository.findById(9L)).thenReturn(Optional.of(asset));

        ApiException exception = assertThrows(ApiException.class, () -> fileService.claimProjectMessageDrafts(
                101L, 20L, new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 7L), java.util.List.of(9L)));

        assertEquals(403, exception.getCode());
        verify(storageRepository, never()).claimProjectMessageDraft(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void projectMessageDraftRejectsAttachmentFromAnotherOrganizationOrProjectContext() {
        FileAssetEntity asset = FileAssetEntity.builder()
                .id(10L).uploaderActorType(ActorRef.ActorType.CUSTOMER_USER).uploaderActorId(7L)
                .organizationId(21L).businessScope(FileBusinessScope.PRIVATE_DRAFT)
                .status(FileStatus.ACTIVE).version(0L).build();
        when(storageRepository.findById(10L)).thenReturn(Optional.of(asset));

        ApiException exception = assertThrows(ApiException.class, () -> fileService.claimProjectMessageDrafts(
                101L, 20L, new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 7L), java.util.List.of(10L)));

        assertEquals(403, exception.getCode());
    }

    @Test
    void publicPortfolioSceneRejectsNonImageFile() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "case.pdf", "application/pdf", new byte[]{1});

        ApiException exception = assertThrows(ApiException.class,
                () -> fileService.upload(pdf, StorageScene.PORTFOLIO_COVER_PUBLIC));

        assertEquals(400, exception.getCode());
        verify(storageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void conversationImageSceneRejectsMismatchedImageExtensionAndMimeType() {
        MockMultipartFile disguisedFile = new MockMultipartFile(
                "file", "preview.jpg", "application/pdf", new byte[]{1});

        ApiException exception = assertThrows(ApiException.class,
                () -> fileService.upload(disguisedFile, StorageScene.CONVERSATION_IMAGE));

        assertEquals(400, exception.getCode());
        verify(storageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void conversationParticipantCanDownloadAttachedFile() throws IOException {
        when(storageRepository.findById(8L)).thenReturn(Optional.of(activeFile(8L, 2L)));
        when(authService.currentUserProfile()).thenReturn(user(1L));
        when(conversationAccessService.isAttachedToConversation(8L)).thenReturn(true);
        when(conversationAccessService.canCurrentUserAccessAttachedFile(8L)).thenReturn(true);
        when(projectAuthorizationService.authorizeProjectFile(8L)).thenReturn(
                new ProjectAuthorizationService.ProjectFileAccessDecision(false, false));
        when(localFileStorage.read(null, "uploads/8/attachment.pdf"))
                .thenReturn("attachment content".getBytes());

        byte[] body = fileService.download(8L);

        assertEquals("attachment content", new String(body));
    }

    @Test
    void unrelatedUserCannotDownloadAttachedFile() {
        when(storageRepository.findById(8L)).thenReturn(Optional.of(activeFile(8L, 2L)));
        when(authService.currentUserProfile()).thenReturn(user(99L));
        when(conversationAccessService.isAttachedToConversation(8L)).thenReturn(true);
        when(conversationAccessService.canCurrentUserAccessAttachedFile(8L)).thenReturn(false);
        when(projectAuthorizationService.authorizeProjectFile(8L)).thenReturn(
                new ProjectAuthorizationService.ProjectFileAccessDecision(false, false));

        ApiException exception = assertThrows(ApiException.class, () -> fileService.download(8L));

        assertEquals(403, exception.getCode());
    }

    @Test
    void formerUploaderCannotDownloadConversationFileAfterLosingAccess()
            throws java.io.IOException {
        when(storageRepository.findById(8L)).thenReturn(Optional.of(activeFile(8L, 2L)));
        when(authService.currentUserProfile()).thenReturn(
                new UserProfile(2L, "原设计师", UserRole.DESIGNER, null, UserStatus.ENABLED));
        when(conversationAccessService.isAttachedToConversation(8L)).thenReturn(true);
        when(conversationAccessService.canCurrentUserAccessAttachedFile(8L)).thenReturn(false);
        when(projectAuthorizationService.authorizeProjectFile(8L)).thenReturn(
                new ProjectAuthorizationService.ProjectFileAccessDecision(false, false));

        ApiException exception = assertThrows(ApiException.class, () -> fileService.download(8L));

        assertEquals(403, exception.getCode());
        verify(localFileStorage, never()).read(null, "uploads/8/attachment.pdf");
    }

    @Test
    void participantCannotDeleteAnotherUsersAttachment() {
        when(storageRepository.findById(8L)).thenReturn(Optional.of(activeFile(8L, 2L)));
        when(authService.currentUserProfile()).thenReturn(user(1L));

        ApiException exception = assertThrows(ApiException.class, () -> fileService.delete(8L));

        assertEquals(403, exception.getCode());
        verify(storageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private FileAssetEntity activeFile(Long id, Long uploaderId) {
        return FileAssetEntity.builder()
                .id(id)
                .uploaderActorType(com.advertisementdesign.back.identity.model.ActorRef.ActorType.CUSTOMER_USER)
                .uploaderActorId(uploaderId)
                .originalName("attachment.pdf")
                .objectKey("uploads/8/attachment.pdf")
                .status(FileStatus.ACTIVE)
                .build();
    }

    private UserProfile user(Long id) {
        return new UserProfile(id, "用户" + id, UserRole.CUSTOMER, null, UserStatus.ENABLED);
    }
}
