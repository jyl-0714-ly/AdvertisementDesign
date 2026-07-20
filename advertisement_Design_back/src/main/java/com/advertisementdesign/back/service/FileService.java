package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.file.FileModels;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.AuthContext;
import com.advertisementdesign.back.domain.entity.FileAssetEntity;
import com.advertisementdesign.back.domain.entity.ProjectEntity;
import com.advertisementdesign.back.domain.entity.ProjectFileEntity;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.FileRole;
import com.advertisementdesign.back.domain.enums.FileStatus;
import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.StorageProvider;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private final DemoDataStore store;
    private final ApiAssembler assembler;
    private final AuthService authService;

    public FileModels.FileAssetVO upload(MultipartFile file) {
        UserEntity currentUser = authService.currentUserEntity();
        String safeName = file.getOriginalFilename() == null ? "file.bin" : file.getOriginalFilename();
        FileAssetEntity asset = FileAssetEntity.builder()
                .uploaderId(currentUser.getId())
                .originalName(safeName)
                .storageName(UUID.randomUUID() + "-" + safeName)
                .storageProvider(StorageProvider.LOCAL)
                .bucketName(null)
                .objectKey("uploads/" + UUID.randomUUID() + "/" + safeName)
                .url("/api/files/temp/" + UUID.randomUUID())
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .fileHash(UUID.randomUUID().toString())
                .status(FileStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return assembler.toFileVO(store.saveFileAsset(asset));
    }

    public FileModels.FileAssetVO detail(Long fileId) {
        return store.findFileAssetById(fileId)
                .map(assembler::toFileVO)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    public byte[] download(Long fileId) {
        FileAssetEntity asset = store.findFileAssetById(fileId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ensureFileAccessible(asset);
        return ("Demo download content for " + asset.getOriginalName()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public boolean delete(Long fileId) {
        FileAssetEntity asset = store.findFileAssetById(fileId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ensureUploaderOrDesigner(asset);
        asset.setStatus(FileStatus.DELETED);
        asset.setUpdatedAt(LocalDateTime.now());
        store.saveFileAsset(asset);
        return true;
    }

    public List<FileModels.ProjectFileVO> listProjectFiles(Long projectId, String stageCode, FileRole fileRole) {
        ProjectEntity project = findAllowedProject(projectId);
        return store.listProjectFiles(project.getId(), stageCode, fileRole).stream()
                .map(assembler::toProjectFileVO)
                .toList();
    }

    public FileModels.ProjectFileVO archiveProjectFile(Long projectId, FileModels.CreateProjectFileRequest request) {
        ProjectEntity project = findAllowedProject(projectId);
        UserEntity currentUser = authService.currentUserEntity();
        if (currentUser.getRole() != UserRole.DESIGNER || !Objects.equals(project.getDesignerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        FileAssetEntity file = store.findFileAssetById(request.fileId()).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ProjectFileEntity entity = ProjectFileEntity.builder()
                .projectId(project.getId())
                .projectStageId(request.projectStageId())
                .stageCode(request.stageCode())
                .fileId(file.getId())
                .uploaderId(currentUser.getId())
                .fileRole(request.fileRole() == null ? FileRole.DELIVERABLE : request.fileRole())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();
        return assembler.toProjectFileVO(store.saveProjectFile(entity));
    }

    public boolean deleteProjectFile(Long projectFileId) {
        ProjectFileEntity projectFile = store.findProjectFileById(projectFileId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ProjectEntity project = findAllowedProject(projectFile.getProjectId());
        UserEntity currentUser = authService.currentUserEntity();
        if (currentUser.getRole() != UserRole.DESIGNER || !Objects.equals(project.getDesignerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return store.deleteProjectFile(projectFileId);
    }

    private ProjectEntity findAllowedProject(Long projectId) {
        ProjectEntity project = store.findProjectById(projectId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserEntity currentUser = authService.currentUserEntity();
        if (currentUser.getRole() == UserRole.CUSTOMER && !Objects.equals(project.getCustomerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (currentUser.getRole() == UserRole.DESIGNER && !Objects.equals(project.getDesignerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return project;
    }

    private void ensureFileAccessible(FileAssetEntity asset) {
        if (asset.getStatus() == FileStatus.DELETED) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
    }

    private void ensureUploaderOrDesigner(FileAssetEntity asset) {
        UserEntity currentUser = authService.currentUserEntity();
        if (!Objects.equals(asset.getUploaderId(), currentUser.getId()) && currentUser.getRole() != UserRole.DESIGNER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
