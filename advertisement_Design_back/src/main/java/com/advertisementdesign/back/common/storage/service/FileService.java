package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.storage.converter.FileConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private final StorageRepository storageRepository;
    private final CommunicationRepository communicationRepository;
    private final ProjectRepository projectRepository;
    private final LocalFileStorage localFileStorage;
    private final FileConverter converter;
    private final AuthService authService;

    @Transactional
    public FileModels.FileAssetVO upload(MultipartFile file) {
        UserProfile currentUser = authService.currentUserProfile();
        String originalName = file.getOriginalFilename() == null ? "file.bin" : file.getOriginalFilename();
        String safeName = java.nio.file.Path.of(originalName).getFileName().toString();
        String objectKey = "uploads/" + UUID.randomUUID() + "/" + safeName;
        FileAssetEntity asset = FileAssetEntity.builder()
                .uploaderId(currentUser.id())
                .originalName(safeName)
                .storageName(UUID.randomUUID() + "-" + safeName)
                .storageProvider(StorageProvider.LOCAL)
                .bucketName(null)
                .objectKey(objectKey)
                .url("/api/files/temp/" + UUID.randomUUID())
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .fileHash(UUID.randomUUID().toString())
                .status(FileStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            localFileStorage.store(objectKey, file);
            return converter.toVO(storageRepository.save(asset));
        } catch (IOException exception) {
            try {
                localFileStorage.delete(objectKey);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new UncheckedIOException("Failed to store uploaded file", exception);
        } catch (RuntimeException exception) {
            try {
                localFileStorage.delete(objectKey);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    public FileModels.FileAssetVO detail(Long fileId) {
        return converter.toVO(findAccessibleAsset(fileId));
    }

    public byte[] download(Long fileId) {
        FileAssetEntity asset = findAccessibleAsset(fileId);
        try {
            return localFileStorage.read(asset.getObjectKey());
        } catch (IOException exception) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
    }

    public boolean delete(Long fileId) {
        FileAssetEntity asset = storageRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserProfile currentUser = authService.currentUserProfile();
        if (!Objects.equals(asset.getUploaderId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        asset.setStatus(FileStatus.DELETED);
        asset.setUpdatedAt(LocalDateTime.now());
        storageRepository.save(asset);
        return true;
    }

    private FileAssetEntity findAccessibleAsset(Long fileId) {
        FileAssetEntity asset = storageRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (asset.getStatus() == FileStatus.DELETED) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        UserProfile currentUser = authService.currentUserProfile();
        if (!Objects.equals(asset.getUploaderId(), currentUser.id())
                && !canAccessAssociatedFile(fileId, currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return asset;
    }

    private boolean canAccessAssociatedFile(Long fileId, Long userId) {
        return communicationRepository.canUserAccessAttachedFile(fileId, userId)
                || projectRepository.canUserAccessFile(fileId, userId);
    }
}
