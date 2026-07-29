package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.outbox.service.ReliableEventWriter;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.advertisementdesign.back.workflow.converter.WorkflowConverter;
import com.advertisementdesign.back.workflow.dto.WorkflowCommandRequests;
import com.advertisementdesign.back.workflow.entity.ProjectStageEventEntity;
import com.advertisementdesign.back.workflow.entity.ProjectStageInstanceEntity;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageEventType;
import com.advertisementdesign.back.workflow.enums.StageStatus;
import com.advertisementdesign.back.workflow.enums.WorkflowCommandType;
import com.advertisementdesign.back.workflow.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowCommandServiceRiskTest {
    private static final long PROJECT_ID = 21L;
    private static final ActorRef ACTOR = new ActorRef(ActorRef.ActorType.ADMIN_USER, 7L);

    @Mock private WorkflowRepository repository;
    @Mock private ProjectQueryService projectQueryService;
    @Mock private ProjectAuthorizationService authorizationService;
    @Mock private CurrentActorProvider currentActorProvider;
    @Mock private ReliableEventWriter eventWriter;

    private WorkflowCommandService service;

    @BeforeEach
    void setUp() {
        service = new WorkflowCommandService(
                repository,
                new WorkflowConverter(),
                projectQueryService,
                authorizationService,
                currentActorProvider,
                List.of(),
                eventWriter,
                new ObjectMapper().findAndRegisterModules());

        when(projectQueryService.findContext(PROJECT_ID)).thenReturn(Optional.of(
                new ProjectModels.ProjectContextView(PROJECT_ID, 3L, ProjectStatus.ACTIVE, 0L)));
        when(authorizationService.authorize(eq(PROJECT_ID), any())).thenReturn(allowed());
        org.mockito.Mockito.lenient().when(currentActorProvider.requireCurrentActor())
                .thenReturn(new CurrentActorProvider.CurrentActor(ACTOR, "管理员"));
        when(repository.findEventByRequestId(anyString())).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(repository.appendEvent(any())).thenAnswer(invocation -> {
            ProjectStageEventEntity event = invocation.getArgument(0);
            event.setId(100L);
            return event;
        });
    }

    @Test
    void cannotActivateStageWhilePrerequisiteIsIncomplete() {
        ProjectStageInstanceEntity first = stage(1L, StageCode.REQUIREMENT_GUIDE, 1,
                StageStatus.ACTIVE, 1, 0L);
        ProjectStageInstanceEntity second = stage(2L, StageCode.CONTRACT_PREPAYMENT, 2,
                StageStatus.NOT_STARTED, 0, 0L);
        when(repository.findStage(PROJECT_ID, 2L)).thenReturn(Optional.of(second));
        when(repository.findStages(PROJECT_ID)).thenReturn(List.of(first, second));

        ApiException exception = assertThrows(ApiException.class, () -> service.execute(
                PROJECT_ID, 2L, request(WorkflowCommandType.ACTIVATE, 0L, "activate-stage-2")));

        assertEquals("前置阶段尚未完成，不能激活当前阶段", exception.getMessage());
        verify(repository, never()).transition(anyLong(), anyLong(), anyLong(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void concurrentCompletionCanCommitOnlyOnce() {
        ProjectStageInstanceEntity firstAttempt = stage(7L, StageCode.AFTER_SALE_REPURCHASE, 7,
                StageStatus.ACTIVE, 1, 4L);
        ProjectStageInstanceEntity competingAttempt = stage(7L, StageCode.AFTER_SALE_REPURCHASE, 7,
                StageStatus.ACTIVE, 1, 4L);
        when(repository.findStage(PROJECT_ID, 7L))
                .thenReturn(Optional.of(firstAttempt), Optional.of(competingAttempt));
        when(repository.transition(eq(PROJECT_ID), eq(7L), eq(4L), eq(StageStatus.ACTIVE),
                eq(StageStatus.COMPLETED), eq(1), isNull(), any(), any()))
                .thenReturn(true, false);

        service.execute(PROJECT_ID, 7L,
                request(WorkflowCommandType.COMPLETE, 4L, "complete-final-a"));
        ApiException exception = assertThrows(ApiException.class, () -> service.execute(
                PROJECT_ID, 7L, request(WorkflowCommandType.COMPLETE, 4L, "complete-final-b")));

        assertEquals("阶段状态发生并发变化，请刷新后重试", exception.getMessage());
        verify(repository, org.mockito.Mockito.times(1)).appendEvent(any());
    }

    @Test
    void reopenPreservesCompletionEventAndCreatesNewActivationRound() {
        ProjectStageInstanceEntity completed = stage(1L, StageCode.REQUIREMENT_GUIDE, 1,
                StageStatus.COMPLETED, 1, 5L);
        completed.setCompletedAt(LocalDateTime.of(2026, 7, 20, 10, 0));
        ProjectStageInstanceEntity later = stage(2L, StageCode.CONTRACT_PREPAYMENT, 2,
                StageStatus.ACTIVE, 1, 2L);
        ProjectStageEventEntity originalCompletion = ProjectStageEventEntity.builder()
                .id(80L).projectId(PROJECT_ID).stageInstanceId(1L)
                .stageCode(StageCode.REQUIREMENT_GUIDE).eventType(StageEventType.COMPLETED)
                .fromStatus(StageStatus.PROCESSING).toStatus(StageStatus.COMPLETED)
                .activationNumber(1).requestId("original-completion")
                .occurredAt(LocalDateTime.of(2026, 7, 20, 10, 0)).build();
        when(repository.findStage(PROJECT_ID, 1L)).thenReturn(Optional.of(completed));
        when(repository.findStages(PROJECT_ID)).thenReturn(List.of(completed, later));
        when(repository.transition(anyLong(), anyLong(), anyLong(), any(), any(),
                any(), any(), any(), any())).thenReturn(true);

        var result = service.execute(PROJECT_ID, 1L,
                request(WorkflowCommandType.REOPEN, 5L, "reopen-requirement"));

        assertEquals(StageStatus.ACTIVE, result.stage().status());
        assertEquals(2, result.stage().activationCount());
        assertEquals(StageEventType.REOPENED, result.event().eventType());
        assertEquals(StageEventType.COMPLETED, originalCompletion.getEventType());
        assertEquals(1, originalCompletion.getActivationNumber());
        ArgumentCaptor<ProjectStageEventEntity> events = ArgumentCaptor.forClass(ProjectStageEventEntity.class);
        verify(repository, org.mockito.Mockito.times(2)).appendEvent(events.capture());
        assertTrue(events.getAllValues().stream().anyMatch(event ->
                event.getEventType() == StageEventType.REOPENED && event.getActivationNumber() == 2));
        assertTrue(events.getAllValues().stream().anyMatch(event ->
                event.getEventType() == StageEventType.SUSPENDED && event.getStageInstanceId().equals(2L)));
    }

    @Test
    void ordinaryCommandCannotBypassSuspension() {
        ProjectStageInstanceEntity suspended = stage(3L, StageCode.RESEARCH_REPORT, 3,
                StageStatus.SUSPENDED, 1, 6L);
        when(repository.findStage(PROJECT_ID, 3L)).thenReturn(Optional.of(suspended));

        ApiException exception = assertThrows(ApiException.class, () -> service.execute(
                PROJECT_ID, 3L,
                request(WorkflowCommandType.START_PROCESSING, 6L, "process-suspended")));

        assertEquals("阶段已暂停，只能先执行恢复命令", exception.getMessage());
        verify(repository, never()).transition(anyLong(), anyLong(), anyLong(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void afterSaleStageMayRemainActiveWithoutForcedCompletion() {
        ProjectStageInstanceEntity afterSale = stage(7L, StageCode.AFTER_SALE_REPURCHASE, 7,
                StageStatus.ACTIVE, 1, 9L);
        when(repository.findStage(PROJECT_ID, 7L)).thenReturn(Optional.of(afterSale));
        when(repository.transition(eq(PROJECT_ID), eq(7L), eq(9L), eq(StageStatus.ACTIVE),
                eq(StageStatus.WAITING_CUSTOMER), eq(1), isNull(), isNull(), any()))
                .thenReturn(true);

        var result = service.execute(PROJECT_ID, 7L,
                request(WorkflowCommandType.WAIT_FOR_CUSTOMER, 9L, "after-sale-waiting"));

        assertEquals(StageStatus.WAITING_CUSTOMER, result.stage().status());
        assertEquals(StageEventType.WAITING, result.event().eventType());
        verify(repository, never()).findStages(PROJECT_ID);
    }

    private WorkflowCommandRequests.Execute request(
            WorkflowCommandType command, long version, String requestId) {
        return new WorkflowCommandRequests.Execute(
                command, version, requestId, "risk test", null, null, null);
    }

    private ProjectStageInstanceEntity stage(
            long id, StageCode code, int order, StageStatus status,
            int activationCount, long version) {
        return ProjectStageInstanceEntity.builder()
                .id(id).projectId(PROJECT_ID).stageCode(code).stageName(code.name())
                .sortOrder(order).status(status).activationCount(activationCount)
                .version(version).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private ProjectAuthorizationService.AuthorizationDecision allowed() {
        return new ProjectAuthorizationService.AuthorizationDecision(
                true,
                ProjectAuthorizationService.AccessLevel.ADMIN,
                new ProjectAuthorizationService.AuthorizationBasis(
                        "TEST", PROJECT_ID, ProjectAuthorizationService.ProjectAction.UPDATE_PROJECT,
                        ACTOR, LocalDateTime.now(), null, null, null, null, Set.of("workflow")));
    }
}
