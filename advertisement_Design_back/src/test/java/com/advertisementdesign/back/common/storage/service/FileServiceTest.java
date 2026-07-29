package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.converter.FileConverter;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.enums.StorageZone;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock
    private StorageRepository storageRepository;
    @Mock
    private LocalFileStorage localFileStorage;
    @Mock
    private FileConverter converter;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(storageRepository, localFileStorage, converter);
    }

    @Test
    void projectMessageDraftRejectsAttachmentUploadedByAnotherUser() {
        FileAssetEntity asset = FileAssetEntity.builder()
                .id(9L).uploaderActorType("CUSTOMER_USER").uploaderActorId(99L)
                .organizationId(20L).businessScope(FileBusinessScope.PRIVATE_DRAFT)
                .storageZone(StorageZone.PRIVATE).status(FileStatus.ACTIVE).version(0L).build();
        when(storageRepository.findById(9L)).thenReturn(Optional.of(asset));

        ApiException exception = assertThrows(ApiException.class, () -> fileService.claimProjectMessageDraft(
                9L, new FileService.Uploader("CUSTOMER_USER", 7L), 20L, 101L));

        assertEquals(403, exception.getCode());
        verify(storageRepository, never()).claimProjectMessageDraft(
                any(), any(), any(), any(), any());
    }

    @Test
    void publicPortfolioSceneRejectsNonImageFile() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "case.pdf", "application/pdf", new byte[]{1});

        ApiException exception = assertThrows(ApiException.class,
                () -> fileService.upload(pdf, StorageScene.PORTFOLIO_COVER_PUBLIC,
                        new FileService.Uploader("DESIGNER_USER", 7L)));

        assertEquals(400, exception.getCode());
        verify(storageRepository, never()).save(any());
    }

    @Test
    void conversationImageSceneRejectsMismatchedImageExtensionAndMimeType() {
        MockMultipartFile disguisedFile = new MockMultipartFile(
                "file", "preview.jpg", "application/pdf", new byte[]{1});

        ApiException exception = assertThrows(ApiException.class,
                () -> fileService.upload(disguisedFile, StorageScene.CONVERSATION_IMAGE,
                        new FileService.Uploader("CUSTOMER_USER", 7L)));

        assertEquals(400, exception.getCode());
        verify(storageRepository, never()).save(any());
    }
}
