package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.project.model.ProjectFileModels;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectFileEntity;
import com.advertisementdesign.back.project.enums.FileRole;
import com.advertisementdesign.back.project.repository.ProjectRepository;
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
class ProjectFileServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private StorageRepository storageRepository;
    @Mock
    private FileService fileService;
    @Mock
    private ProjectConverter converter;
    @Mock
    private AuthService authService;

    private ProjectFileService projectFileService;

    @BeforeEach
    void setUp() {
        projectFileService = new ProjectFileService(
                projectRepository, storageRepository, fileService, converter, authService);
    }

    @Test
    void assignedDesignerUploadsProjectRolesToControlledPrivateScenes() {
        ProjectEntity project = project();
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf", new byte[]{1});
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project));
        when(authService.currentUserProfile()).thenReturn(user(2L));

        projectFileService.uploadProjectFile(1L, FileRole.CONTRACT, file);

        verify(fileService).upload(file, StorageScene.PROJECT_CONTRACT);
    }

    @Test
    void unassignedDesignerCannotUploadProjectFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "draft.pdf", "application/pdf", new byte[]{1});
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project()));
        when(authService.currentUserProfile()).thenReturn(user(3L));

        ApiException exception = assertThrows(ApiException.class,
                () -> projectFileService.uploadProjectFile(1L, FileRole.DRAFT, file));

        assertEquals(403, exception.getCode());
        verify(fileService, never()).upload(any(), any(StorageScene.class));
    }

    @Test
    void assignedDesignerCanArchiveProjectFile() {
        ProjectEntity project = project();
        ProjectFileModels.CreateProjectFileRequest request =
                new ProjectFileModels.CreateProjectFileRequest(8L, 3L, "RESEARCH_REPORT", FileRole.DELIVERABLE, "交付稿");
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project));
        when(authService.currentUserProfile()).thenReturn(user(2L));
        when(storageRepository.findById(8L)).thenReturn(Optional.of(FileAssetEntity.builder()
                .id(8L)
                .uploaderId(2L)
                .build()));
        when(projectRepository.existsProjectFile(1L, 8L)).thenReturn(false);
        when(projectRepository.saveProjectFile(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.toProjectFileVO(any())).thenReturn(projectFileVO());

        projectFileService.archiveProjectFile(1L, request);

        verify(projectRepository).saveProjectFile(any());
    }

    @Test
    void assignedDesignerCannotArchiveAnotherUsersFile() {
        ProjectFileModels.CreateProjectFileRequest request =
                new ProjectFileModels.CreateProjectFileRequest(
                        8L, null, "DRAFT", FileRole.DRAFT, null);
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project()));
        when(authService.currentUserProfile()).thenReturn(user(2L));
        when(storageRepository.findById(8L)).thenReturn(Optional.of(FileAssetEntity.builder()
                .id(8L)
                .uploaderId(99L)
                .build()));

        ApiException exception = assertThrows(ApiException.class,
                () -> projectFileService.archiveProjectFile(1L, request));

        assertEquals(403, exception.getCode());
        verify(projectRepository, never()).saveProjectFile(any());
    }

    @Test
    void unassignedDesignerCannotDeleteProjectFile() {
        when(projectRepository.findProjectFileById(9L)).thenReturn(Optional.of(ProjectFileEntity.builder()
                .id(9L)
                .projectId(1L)
                .build()));
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project()));
        when(authService.currentUserProfile()).thenReturn(user(3L));

        ApiException exception = assertThrows(ApiException.class,
                () -> projectFileService.deleteProjectFile(9L));

        assertEquals(403, exception.getCode());
        verify(projectRepository, never()).deleteProjectFile(any());
    }

    private ProjectEntity project() {
        return ProjectEntity.builder()
                .id(1L)
                .customerId(1L)
                .designerId(2L)
                .build();
    }

    private UserProfile user(Long id) {
        return new UserProfile(id, "用户" + id, UserRole.DESIGNER, null, UserStatus.ENABLED);
    }

    private ProjectFileModels.ProjectFileVO projectFileVO() {
        return new ProjectFileModels.ProjectFileVO(
                9L, 1L, 3L, "RESEARCH_REPORT", 8L, 2L,
                FileRole.DELIVERABLE, "交付稿", null, null);
    }
}
