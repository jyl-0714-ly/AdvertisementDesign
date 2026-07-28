package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.audit.repository.AuditRepository;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.service.UnifiedConversationService;
import com.advertisementdesign.back.consultation.model.ProjectPreparationModels;
import com.advertisementdesign.back.consultation.service.ProjectPreparationService;
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
import org.mockito.ArgumentCaptor;
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
    @Mock private AuditRepository auditRepository;
    @Mock private UnifiedConversationService unifiedConversationService;
    @Mock private ProjectConverter converter;
    @Mock private AuthService authService;
    @Mock private ProjectPreparationService projectPreparationService;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, auditRepository, unifiedConversationService,
                converter, authService, projectPreparationService);
    }

    @Test
    void createRejectsMissingContract() {
        when(projectPreparationService.lockForProjectCreation(7L)).thenReturn(preparation(false, true));
        ApiException exception = assertThrows(ApiException.class,
                () -> service.createFromConsultation(request()));
        assertEquals(400, exception.getCode());
        verify(projectRepository, never()).saveProject(any());
    }

    @Test
    void createRejectsMissingInitialPayment() {
        when(projectPreparationService.lockForProjectCreation(7L)).thenReturn(preparation(true, false));
        ApiException exception = assertThrows(ApiException.class,
                () -> service.createFromConsultation(request()));
        assertEquals(400, exception.getCode());
        verify(projectRepository, never()).saveProject(any());
    }

    @Test
    void createRejectsDuplicateConsultationProject() {
        when(projectPreparationService.lockForProjectCreation(7L)).thenReturn(preparation(true, true));
        when(projectRepository.findProjectByConsultantIntakeId(7L)).thenReturn(Optional.of(project()));
        assertEquals(400, assertThrows(ApiException.class,
                () -> service.createFromConsultation(request())).getCode());
        verify(projectRepository, never()).saveProject(any());
    }

    @Test
    void createDerivesParticipantsFromPreparation() {
        when(projectPreparationService.lockForProjectCreation(7L)).thenReturn(preparation(true, true));
        when(projectRepository.findProjectByConsultantIntakeId(7L)).thenReturn(Optional.empty());
        when(projectRepository.saveProject(any())).thenAnswer(invocation -> {
            ProjectEntity project = invocation.getArgument(0);
            project.setId(99L);
            return project;
        });
        when(projectRepository.findStage(any(), any())).thenReturn(Optional.empty());
        when(converter.toProjectVO(any())).thenReturn(new ProjectModels.ProjectVO(
                99L, "正式项目", "说明", 11L, null, 22L, null, 7L,
                "REQUIREMENT_GUIDE", "需求引导", ProjectStatus.IN_PROGRESS, 0,
                LocalDateTime.now().toString(), LocalDateTime.now().toString()));

        ProjectModels.ProjectVO result = service.createFromConsultation(request());

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).saveProject(captor.capture());
        assertEquals(11L, captor.getValue().getCustomerId());
        assertEquals(22L, captor.getValue().getDesignerId());
        assertEquals(7L, captor.getValue().getConsultantIntakeId());
        verify(unifiedConversationService).bindProject(7L, 99L, 11L, 22L);
        assertEquals(99L, result.id());
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

    private ProjectModels.CreateProjectFromConsultationRequest request() {
        return new ProjectModels.CreateProjectFromConsultationRequest(7L, "正式项目", "说明");
    }

    private ProjectPreparationModels.ProjectPreparation preparation(boolean contract, boolean payment) {
        return new ProjectPreparationModels.ProjectPreparation(
                7L, 11L, 22L, "品牌设计", "需求", contract,
                contract ? LocalDateTime.now() : null, payment,
                payment ? LocalDateTime.now() : null);
    }

    private ProjectEntity project() {
        return ProjectEntity.builder().id(99L).name("项目").customerId(11L).designerId(22L)
                .consultantIntakeId(7L).currentStage("REQUIREMENT_GUIDE")
                .status(ProjectStatus.IN_PROGRESS).progress(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
