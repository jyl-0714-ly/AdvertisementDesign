package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectConverter converter;
    @Mock private AuthService authService;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, converter, authService);
    }

    @Test
    void updateRejectsDesignerReassignment() {
        when(authService.currentUserProfile()).thenReturn(
                new UserProfile(22L, "设计师", UserRole.DESIGNER, null, UserStatus.ENABLED));
        when(projectRepository.findProjectById(99L)).thenReturn(Optional.of(project()));
        ApiException exception = assertThrows(ApiException.class, () -> service.update(
                99L, new ProjectModels.UpdateProjectRequest(33L, null, null, null)));
        assertEquals(400, exception.getCode());
        verify(projectRepository, never()).saveProject(any());
    }

    private ProjectEntity project() {
        return ProjectEntity.builder().id(99L).name("项目").customerId(11L).designerId(22L)
                .currentStage("REQUIREMENT_GUIDE")
                .status(ProjectStatus.IN_PROGRESS).progress(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
