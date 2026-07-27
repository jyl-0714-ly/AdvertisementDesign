package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.audit.repository.AuditRepository;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectStageEntity;
import com.advertisementdesign.back.project.entity.StageActionEntity;
import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.enums.StageActionStatus;
import com.advertisementdesign.back.project.model.StageModels;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AuditRepository auditRepository;
    @Mock
    private CommunicationRepository communicationRepository;
    @Mock
    private ProjectConverter converter;
    @Mock
    private AuthService authService;
    @Mock
    private IdentityService identityService;

    private StageService stageService;

    @BeforeEach
    void setUp() {
        stageService = new StageService(
                projectRepository, auditRepository, communicationRepository,
                converter, authService, identityService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createActionPersistsPendingActionAndStageSideEffects() {
        authenticate(2L, UserRole.DESIGNER);
        ProjectEntity project = project();
        ProjectStageEntity stage = stage(ProjectStageStatus.TODO);
        UserProfile designer = user(2L, UserRole.DESIGNER);
        ConversationEntity conversation = conversation();
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.findStage(1L, "RESEARCH_REPORT")).thenReturn(Optional.of(stage));
        when(authService.currentUserProfile()).thenReturn(designer);
        when(identityService.findById(1L)).thenReturn(Optional.of(new IdentityService.UserProfile(
                1L, "用户1", UserRole.CUSTOMER, null,
                com.advertisementdesign.back.identity.enums.UserStatus.ENABLED)));
        when(projectRepository.saveStageAction(any())).thenAnswer(invocation -> {
            StageActionEntity action = invocation.getArgument(0);
            action.setId(9L);
            return action;
        });
        when(communicationRepository.findConversationByProjectId(1L)).thenReturn(Optional.of(conversation));
        when(communicationRepository.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(communicationRepository.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.toStageActionVO(any())).thenReturn(stageActionVO(9L, StageActionStatus.PENDING));

        StageModels.StageActionVO result = stageService.createAction(
                1L, "RESEARCH_REPORT", new StageModels.CreateStageActionRequest("请确认"));

        ArgumentCaptor<StageActionEntity> actionCaptor = ArgumentCaptor.forClass(StageActionEntity.class);
        verify(projectRepository).saveStageAction(actionCaptor.capture());
        assertEquals(2L, actionCaptor.getValue().getInitiatorId());
        assertEquals(1L, actionCaptor.getValue().getConfirmUserId());
        assertEquals(StageActionStatus.PENDING, actionCaptor.getValue().getStatus());
        assertEquals(ProjectStageStatus.PENDING_CONFIRM, stage.getStatus());
        assertEquals("RESEARCH_REPORT", project.getCurrentStage());
        verify(projectRepository).saveStage(stage);
        verify(projectRepository).saveProject(project);
        verify(communicationRepository).saveMessage(any());
        verify(communicationRepository).saveConversation(conversation);
        verify(auditRepository).save(any());
        assertEquals(9L, result.id());
    }

    @Test
    void confirmReachesStageAndRefreshesProgress() {
        authenticate(1L, UserRole.CUSTOMER);
        StageActionEntity action = pendingAction(1L);
        ProjectStageEntity stage = stage(ProjectStageStatus.PENDING_CONFIRM);
        when(projectRepository.findStageActionById(9L)).thenReturn(Optional.of(action));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));
        when(projectRepository.findStageById(3L)).thenReturn(Optional.of(stage));
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project()));
        when(communicationRepository.findConversationByProjectId(1L)).thenReturn(Optional.of(conversation()));
        when(communicationRepository.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(communicationRepository.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.toStageActionVO(any())).thenReturn(stageActionVO(9L, StageActionStatus.CONFIRMED));

        stageService.confirm(9L, new StageModels.StageActionResponseRequest("确认"));

        assertEquals(StageActionStatus.CONFIRMED, action.getStatus());
        assertEquals(ProjectStageStatus.REACHED, stage.getStatus());
        assertNotNull(stage.getReachedAt());
        verify(projectRepository).saveStageAction(action);
        verify(projectRepository).saveStage(stage);
        verify(projectRepository).refreshProjectProgress(1L);
        verify(communicationRepository).saveMessage(any());
        verify(auditRepository).save(any());
    }

    @Test
    void rejectMarksStageRejectedAndRefreshesProgress() {
        authenticate(1L, UserRole.CUSTOMER);
        StageActionEntity action = pendingAction(1L);
        ProjectStageEntity stage = stage(ProjectStageStatus.PENDING_CONFIRM);
        when(projectRepository.findStageActionById(9L)).thenReturn(Optional.of(action));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));
        when(projectRepository.findStageById(3L)).thenReturn(Optional.of(stage));
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project()));
        when(communicationRepository.findConversationByProjectId(1L)).thenReturn(Optional.of(conversation()));
        when(communicationRepository.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(communicationRepository.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.toStageActionVO(any())).thenReturn(stageActionVO(9L, StageActionStatus.REJECTED));

        stageService.reject(9L, new StageModels.StageActionResponseRequest("补充资料"));

        assertEquals(StageActionStatus.REJECTED, action.getStatus());
        assertEquals(ProjectStageStatus.REJECTED, stage.getStatus());
        verify(projectRepository).saveStageAction(action);
        verify(projectRepository).saveStage(stage);
        verify(projectRepository).refreshProjectProgress(1L);
    }

    @Test
    void nonConfirmUserCannotConfirmOrReject() {
        authenticate(2L, UserRole.DESIGNER);
        StageActionEntity action = pendingAction(1L);
        when(projectRepository.findStageActionById(9L)).thenReturn(Optional.of(action));
        when(authService.currentUserProfile()).thenReturn(user(2L, UserRole.DESIGNER));

        ApiException confirmException = assertThrows(ApiException.class,
                () -> stageService.confirm(9L, new StageModels.StageActionResponseRequest("确认")));
        ApiException rejectException = assertThrows(ApiException.class,
                () -> stageService.reject(9L, new StageModels.StageActionResponseRequest("驳回")));

        assertEquals(403, confirmException.getCode());
        assertEquals(403, rejectException.getCode());
        verify(projectRepository, never()).saveStageAction(any());
        verify(projectRepository, never()).refreshProjectProgress(any());
    }

    @Test
    void unrelatedUsersCannotReadOrCreateStageActions() {
        authenticate(99L, UserRole.CUSTOMER);
        when(projectRepository.findProjectById(1L)).thenReturn(Optional.of(project()));

        ApiException listException = assertThrows(ApiException.class,
                () -> stageService.list(1L, null, null));
        ApiException createException = assertThrows(ApiException.class,
                () -> stageService.createAction(1L, "RESEARCH_REPORT", null));

        assertEquals(403, listException.getCode());
        assertEquals(403, createException.getCode());
        verify(projectRepository, never()).listStageActions(any(), any(), any());
        verify(projectRepository, never()).saveStageAction(any());
    }

    @Test
    void completedActionCannotBeConfirmedAgain() {
        authenticate(1L, UserRole.CUSTOMER);
        StageActionEntity action = pendingAction(1L);
        action.setStatus(StageActionStatus.CONFIRMED);
        when(projectRepository.findStageActionById(9L)).thenReturn(Optional.of(action));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));

        ApiException exception = assertThrows(ApiException.class,
                () -> stageService.confirm(9L, null));

        assertEquals(400, exception.getCode());
        verify(projectRepository, never()).saveStageAction(any());
        verify(projectRepository, never()).refreshProjectProgress(any());
    }

    private void authenticate(Long id, UserRole role) {
        CurrentUser currentUser = CurrentUser.builder()
                .id(id)
                .email("stage-test@example.com")
                .nickname("测试用户")
                .role(role)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
    }

    private ProjectEntity project() {
        return ProjectEntity.builder()
                .id(1L)
                .name("测试项目")
                .customerId(1L)
                .designerId(2L)
                .description("测试")
                .currentStage("RESEARCH_REPORT")
                .status(ProjectStatus.IN_PROGRESS)
                .progress(28)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ProjectStageEntity stage(ProjectStageStatus status) {
        return ProjectStageEntity.builder()
                .id(3L)
                .projectId(1L)
                .stageCode("RESEARCH_REPORT")
                .stageName("资料调研报告")
                .sortOrder(3)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private StageActionEntity pendingAction(Long confirmUserId) {
        return StageActionEntity.builder()
                .id(9L)
                .projectId(1L)
                .projectStageId(3L)
                .stageCode("RESEARCH_REPORT")
                .initiatorId(2L)
                .confirmUserId(confirmUserId)
                .status(StageActionStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ConversationEntity conversation() {
        return ConversationEntity.builder()
                .id(1L)
                .projectId(1L)
                .customerId(1L)
                .designerId(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserProfile user(Long id, UserRole role) {
        return new UserProfile(id, "用户" + id, role, null, UserStatus.ENABLED);
    }

    private StageModels.StageActionVO stageActionVO(Long id, StageActionStatus status) {
        return new StageModels.StageActionVO(
                id, 1L, 3L, "RESEARCH_REPORT", 2L,
                com.advertisementdesign.back.communication.enums.MessageSenderRole.DESIGNER,
                1L, status, "请确认", null, null, null, null, null);
    }
}
