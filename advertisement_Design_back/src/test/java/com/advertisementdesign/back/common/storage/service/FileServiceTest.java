package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.storage.converter.FileConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.project.repository.ProjectRepository;
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
    private CommunicationRepository communicationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private LocalFileStorage localFileStorage;
    @Mock
    private FileConverter converter;
    @Mock
    private AuthService authService;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(
                storageRepository, communicationRepository, projectRepository,
                localFileStorage, converter, authService);
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
        when(communicationRepository.canUserAccessAttachedFile(8L, 1L)).thenReturn(true);
        when(localFileStorage.read(null, "uploads/8/attachment.pdf"))
                .thenReturn("attachment content".getBytes());

        byte[] body = fileService.download(8L);

        assertEquals("attachment content", new String(body));
        verify(projectRepository, never()).canUserAccessFile(8L, 1L);
    }

    @Test
    void unrelatedUserCannotDownloadAttachedFile() {
        when(storageRepository.findById(8L)).thenReturn(Optional.of(activeFile(8L, 2L)));
        when(authService.currentUserProfile()).thenReturn(user(99L));
        when(communicationRepository.canUserAccessAttachedFile(8L, 99L)).thenReturn(false);
        when(projectRepository.canUserAccessFile(8L, 99L)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> fileService.download(8L));

        assertEquals(403, exception.getCode());
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
                .uploaderId(uploaderId)
                .originalName("attachment.pdf")
                .objectKey("uploads/8/attachment.pdf")
                .status(FileStatus.ACTIVE)
                .build();
    }

    private UserProfile user(Long id) {
        return new UserProfile(id, "用户" + id, UserRole.CUSTOMER, null, UserStatus.ENABLED);
    }
}
