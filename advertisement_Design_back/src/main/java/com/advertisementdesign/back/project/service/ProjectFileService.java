package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectFileEntity;
import com.advertisementdesign.back.project.enums.FileRole;
import com.advertisementdesign.back.project.model.ProjectFileModels;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectFileService {
    private final ProjectRepository projectRepository;
    private final StorageRepository storageRepository;
    private final FileService fileService;
    private final ProjectConverter converter;
    private final AuthService authService;

    public List<ProjectFileModels.ProjectFileVO> listProjectFiles(Long projectId, String stageCode, FileRole fileRole) {
        ProjectEntity project = findAllowedProject(projectId);
        return projectRepository.listProjectFiles(project.getId(), stageCode, fileRole).stream()
                .map(converter::toProjectFileVO)
                .toList();
    }

    @Transactional
    public FileModels.FileAssetVO uploadProjectFile(
            Long projectId,
            FileRole fileRole,
            MultipartFile file) {
        ProjectEntity project = findAllowedProject(projectId);
        requireAssignedDesigner(project);
        return fileService.upload(file, storageScene(fileRole));
    }

    @Transactional
    public ProjectFileModels.ProjectFileVO archiveProjectFile(Long projectId, ProjectFileModels.CreateProjectFileRequest request) {
        ProjectEntity project = findAllowedProject(projectId);
        UserProfile currentUser = requireAssignedDesigner(project);
        FileAssetEntity file = storageRepository.findById(request.fileId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (file.getStatus() == FileStatus.DELETED) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        if (!Objects.equals(file.getUploaderId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (projectRepository.existsProjectFile(project.getId(), file.getId())) {
            throw new ApiException(400, "该文件已归档到项目");
        }
        ProjectFileEntity entity = ProjectFileEntity.builder()
                .projectId(project.getId())
                .projectStageId(request.projectStageId())
                .stageCode(request.stageCode())
                .fileId(file.getId())
                .uploaderId(currentUser.id())
                .fileRole(request.fileRole() == null ? FileRole.DELIVERABLE : request.fileRole())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();
        return converter.toProjectFileVO(projectRepository.saveProjectFile(entity));
    }

    @Transactional
    public boolean deleteProjectFile(Long projectFileId) {
        ProjectFileEntity projectFile = projectRepository.findProjectFileById(projectFileId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ProjectEntity project = findAllowedProject(projectFile.getProjectId());
        requireAssignedDesigner(project);
        return projectRepository.deleteProjectFile(projectFileId);
    }

    private StorageScene storageScene(FileRole fileRole) {
        FileRole effectiveRole = fileRole == null ? FileRole.DELIVERABLE : fileRole;
        return switch (effectiveRole) {
            case CONTRACT -> StorageScene.PROJECT_CONTRACT;
            case MATERIAL -> StorageScene.PROJECT_REQUIREMENT_MATERIAL;
            case REPORT -> StorageScene.PROJECT_REPORT;
            case DRAFT -> StorageScene.PROJECT_DRAFT;
            case FINAL -> StorageScene.PROJECT_FINAL;
            case DELIVERABLE -> StorageScene.PROJECT_DELIVERABLE;
            case OTHER -> StorageScene.PROJECT_OTHER;
        };
    }

    private ProjectEntity findAllowedProject(Long projectId) {
        ProjectEntity project = projectRepository.findProjectById(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserProfile currentUser = authService.currentUserProfile();
        if (currentUser.role() == UserRole.CUSTOMER
                && !Objects.equals(project.getCustomerId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (currentUser.role() == UserRole.DESIGNER
                && !Objects.equals(project.getDesignerId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return project;
    }

    private UserProfile requireAssignedDesigner(ProjectEntity project) {
        UserProfile currentUser = authService.currentUserProfile();
        if (currentUser.role() != UserRole.DESIGNER
                || !Objects.equals(project.getDesignerId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return currentUser;
    }
}
