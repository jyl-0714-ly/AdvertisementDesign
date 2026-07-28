package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.communication.service.UnifiedConversationService;
import com.advertisementdesign.back.consultation.config.ConsultationMatchingProperties;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.ConsultationDesignerMatchEntity;
import com.advertisementdesign.back.consultation.entity.DesignerProfileEntity;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchType;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.enums.DesignerAvailabilityStatus;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationMatchingServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Instant NOW = Instant.parse("2026-07-28T02:00:03Z");

    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private IdentityService identityService;
    @Mock
    private UnifiedConversationService unifiedConversationService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final Map<Long, UserProfile> users = new HashMap<>();
    private final List<DesignerProfileEntity> profiles = new ArrayList<>();
    private final Map<Long, Long> workloads = new HashMap<>();
    private Clock clock;
    private ConsultationMatchingService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZONE);
        service = new ConsultationMatchingService(
                consultationRepository,
                projectRepository,
                identityService,
                unifiedConversationService,
                new ConsultationMatchingProperties(
                        Duration.ofSeconds(3), Duration.ofSeconds(60), Duration.ofMillis(500)),
                clock,
                transactionManager);
        org.mockito.Mockito.lenient().when(consultationRepository.hasPendingMatch(anyLong()))
                .thenReturn(false);
        org.mockito.Mockito.lenient().when(consultationRepository.listFailedDesignerIds(anyLong()))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(consultationRepository.listEnabledDesignerProfiles())
                .thenAnswer(invocation -> profiles);
        org.mockito.Mockito.lenient().when(consultationRepository.findDesignerProfileForUpdate(anyLong()))
                .thenAnswer(invocation -> profiles.stream()
                        .filter(profile -> profile.getDesignerId().equals(
                                invocation.getArgument(0, Long.class)))
                        .findFirst());
        org.mockito.Mockito.lenient().when(identityService.findById(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(users.get(invocation.getArgument(0, Long.class))));
        org.mockito.Mockito.lenient().when(projectRepository.countInProgressProjectsByDesigner(anyLong()))
                .thenAnswer(invocation -> workloads.getOrDefault(
                        invocation.getArgument(0, Long.class), 0L));
        org.mockito.Mockito.lenient().when(consultationRepository.countActiveIntakesByDesigner(anyLong()))
                .thenReturn(0L);
        org.mockito.Mockito.lenient().when(consultationRepository.saveDesignerMatch(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(consultationRepository.saveIntake(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(consultationRepository.saveDesignerProfile(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assignmentIsUnavailableBeforeThreeSecondsAndEligibleAtBoundary() {
        ConsultantIntakeEntity intake = dueIntake(7L, LocalDateTime.now(clock).minusSeconds(2));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));

        assertFalse(service.assignDueIntake(7L, ConsultationMatchType.NORMAL));
        verify(consultationRepository, never()).saveDesignerMatch(any());

        addDesigner(2L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 3, List.of("品牌设计"), LocalDateTime.now(clock).minusMinutes(10));
        intake.setHandoffConfirmedAt(LocalDateTime.now(clock).minusSeconds(3));
        intake.setNextMatchAt(LocalDateTime.now(clock));

        assertTrue(service.assignDueIntake(7L, ConsultationMatchType.NORMAL));
        assertEquals(ConsultantIntakeStatus.MATCHED, intake.getStatus());
        assertEquals(2L, intake.getMatchedDesignerId());
        assertEquals(2L, intake.getInitialDesignerId());
        assertEquals(1, intake.getMatchAttemptCount());
        ArgumentCaptor<ConsultationDesignerMatchEntity> matchCaptor =
                ArgumentCaptor.forClass(ConsultationDesignerMatchEntity.class);
        verify(consultationRepository).saveDesignerMatch(matchCaptor.capture());
        ConsultationDesignerMatchEntity match = matchCaptor.getValue();
        assertEquals(ConsultationMatchStatus.PENDING_ACK, match.getStatus());
        assertEquals(ConsultationMatchType.NORMAL, match.getMatchType());
        assertEquals(LocalDateTime.now(clock).plusSeconds(60), match.getExpiresAt());
        assertNull(intake.getDesignerAcknowledgedAt());
        assertEquals(ConsultantIntakeStatus.MATCHED, intake.getStatus());
        verify(unifiedConversationService).ensureConsultationConversation(
                7L, 1L, 2L, intake.getGreetingMessages());
    }

    @Test
    void noCandidateDefersIntakeSoLaterDueRowsCanRotateIntoBatch() {
        ConsultantIntakeEntity intake = dueIntake(
                7L, LocalDateTime.now(clock).minusSeconds(3));
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));

        assertFalse(service.assignDueIntake(
                7L, ConsultationMatchType.NORMAL));

        verify(consultationRepository).saveIntake(intake);
        assertEquals(1, intake.getMatchAttemptCount());
        assertEquals(LocalDateTime.now(clock).plusSeconds(5),
                intake.getNextMatchAt());
        ArgumentCaptor<ConsultationDesignerMatchEntity> captor =
                ArgumentCaptor.forClass(ConsultationDesignerMatchEntity.class);
        verify(consultationRepository).saveDesignerMatch(captor.capture());
        assertEquals(ConsultationMatchStatus.NO_CANDIDATE,
                captor.getValue().getStatus());
        assertEquals("NO_ELIGIBLE_DESIGNER",
                captor.getValue().getScoreDetail().get("reason"));
    }

    @Test
    void hardFiltersDisabledLeaveStoppedAndCapacityCandidates() {
        ConsultantIntakeEntity intake = dueIntake(7L, LocalDateTime.now(clock).minusSeconds(3));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        addDesigner(2L, false, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 3, List.of("品牌设计"), null);
        addDesigner(3L, true, DesignerAvailabilityStatus.ON_LEAVE, true,
                2, 3, List.of("品牌设计"), null);
        addDesigner(4L, true, DesignerAvailabilityStatus.STOPPED, true,
                2, 3, List.of("品牌设计"), null);
        addDesigner(5L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                1, 1, List.of("品牌设计"), null);
        workloads.put(5L, 1L);
        addDesigner(6L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 3, List.of("包装设计"), null);

        assertTrue(service.assignDueIntake(7L, ConsultationMatchType.NORMAL));

        assertEquals(6L, intake.getMatchedDesignerId());
    }

    @Test
    void fallsBackToNonDutyDesignerAndPersistsFallbackType() {
        ConsultantIntakeEntity intake = dueIntake(7L, LocalDateTime.now(clock).minusSeconds(3));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        addDesigner(2L, true, DesignerAvailabilityStatus.AVAILABLE, false,
                2, 3, List.of("品牌设计"), null);

        assertTrue(service.assignDueIntake(7L, ConsultationMatchType.NORMAL));

        ArgumentCaptor<ConsultationDesignerMatchEntity> captor =
                ArgumentCaptor.forClass(ConsultationDesignerMatchEntity.class);
        verify(consultationRepository).saveDesignerMatch(captor.capture());
        assertEquals(ConsultationMatchType.ON_DUTY_FALLBACK, captor.getValue().getMatchType());
    }

    @Test
    void ambiguousOtherRequirementGetsNeutralSpecialtyScoreAndStillMatches() {
        ConsultantIntakeEntity intake = dueIntake(7L, LocalDateTime.now(clock).minusSeconds(3));
        intake.setProjectType("其他");
        intake.setRequirementDescription("不清楚");
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        addDesigner(2L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 3, List.of("包装设计"), null);

        assertTrue(service.assignDueIntake(7L, ConsultationMatchType.NORMAL));

        ArgumentCaptor<ConsultationDesignerMatchEntity> captor =
                ArgumentCaptor.forClass(ConsultationDesignerMatchEntity.class);
        verify(consultationRepository).saveDesignerMatch(captor.capture());
        assertEquals(new BigDecimal("50"), captor.getValue().getSpecialtyScore());
        assertEquals(new BigDecimal("30"), captor.getValue().getRequirementConfidence());
        assertEquals(Boolean.TRUE, captor.getValue().getScoreDetail().get("specialtyNeutral"));
    }

    @Test
    void weightedScoreUsesActivitySoftCapacitySpecialtyAndFairness() {
        ConsultantIntakeEntity intake = dueIntake(7L, LocalDateTime.now(clock).minusSeconds(3));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        addDesigner(2L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 4, List.of("品牌设计"), null);
        workloads.put(2L, 1L);

        assertTrue(service.assignDueIntake(7L, ConsultationMatchType.NORMAL));

        ArgumentCaptor<ConsultationDesignerMatchEntity> captor =
                ArgumentCaptor.forClass(ConsultationDesignerMatchEntity.class);
        verify(consultationRepository).saveDesignerMatch(captor.capture());
        ConsultationDesignerMatchEntity match = captor.getValue();
        assertEquals(new BigDecimal("100"), match.getActivityScore());
        assertEquals(new BigDecimal("50.00"), match.getWorkloadScore());
        assertEquals(new BigDecimal("100"), match.getSpecialtyScore());
        assertEquals(new BigDecimal("100"), match.getFairnessScore());
        assertEquals(new BigDecimal("85.00"), match.getTotalScore());
        assertEquals(2, match.getScoreDetail().get("softCapacity"));
        assertEquals(4, match.getScoreDetail().get("hardCapacity"));
    }

    @Test
    void retryExcludesFailedDesignerAndReusesSameConversation() {
        ConsultantIntakeEntity intake = dueIntake(7L, LocalDateTime.now(clock).minusSeconds(3));
        intake.setMatchedDesignerId(2L);
        intake.setInitialDesignerId(2L);
        intake.setMatchAttemptCount(1);
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        when(consultationRepository.listFailedDesignerIds(7L)).thenReturn(List.of(2L));
        addDesigner(2L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 3, List.of("品牌设计"), null);
        addDesigner(3L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 3, List.of("品牌设计"), null);

        assertTrue(service.assignDueIntake(7L, ConsultationMatchType.RETRY));

        assertEquals(3L, intake.getMatchedDesignerId());
        assertEquals(2L, intake.getInitialDesignerId());
        assertEquals(2, intake.getMatchAttemptCount());
        verify(unifiedConversationService).ensureConsultationConversation(
                7L, 1L, 3L, intake.getGreetingMessages());
    }

    @Test
    void retryFallbackKeepsRetryType() {
        ConsultantIntakeEntity intake = dueIntake(
                7L, LocalDateTime.now(clock).minusSeconds(3));
        intake.setMatchedDesignerId(2L);
        intake.setInitialDesignerId(2L);
        intake.setMatchAttemptCount(1);
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));
        when(consultationRepository.listFailedDesignerIds(7L))
                .thenReturn(List.of(2L));
        addDesigner(3L, true, DesignerAvailabilityStatus.AVAILABLE, false,
                2, 3, List.of("品牌设计"), null);

        assertTrue(service.assignDueIntake(7L, ConsultationMatchType.RETRY));

        ArgumentCaptor<ConsultationDesignerMatchEntity> captor =
                ArgumentCaptor.forClass(ConsultationDesignerMatchEntity.class);
        verify(consultationRepository).saveDesignerMatch(captor.capture());
        assertEquals(ConsultationMatchType.RETRY, captor.getValue().getMatchType());
    }

    @Test
    void conversationFailurePropagatesAfterAssignmentWritesForTransactionRollback() {
        ConsultantIntakeEntity intake = dueIntake(
                7L, LocalDateTime.now(clock).minusSeconds(3));
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));
        addDesigner(2L, true, DesignerAvailabilityStatus.AVAILABLE, true,
                2, 3, List.of("品牌设计"), null);
        when(unifiedConversationService.ensureConsultationConversation(
                7L, 1L, 2L, List.of(
                        "您好，我是设计师2，接下来由我与您确认需求细节并推进设计方案。",
                        "您的需求已整理完成，请稍等。")))
                .thenThrow(new IllegalStateException("conversation failure"));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.assignDueIntake(7L, ConsultationMatchType.NORMAL));

        InOrder writes = inOrder(
                consultationRepository, unifiedConversationService);
        writes.verify(consultationRepository).saveDesignerMatch(any());
        writes.verify(consultationRepository).saveIntake(intake);
        writes.verify(consultationRepository).saveDesignerProfile(any());
        writes.verify(unifiedConversationService)
                .ensureConsultationConversation(
                        7L, 1L, 2L, intake.getGreetingMessages());
    }

    @Test
    void scheduledScannerContinuesAfterOneAssignmentFails() {
        ConsultantIntakeEntity first = dueIntake(
                7L, LocalDateTime.now(clock).minusSeconds(3));
        ConsultantIntakeEntity second = dueIntake(
                8L, LocalDateTime.now(clock).minusSeconds(3));
        when(consultationRepository.listDueHandoffIntakes(
                LocalDateTime.now(clock), 50))
                .thenReturn(List.of(first, second));
        ConsultationMatchingService spyService =
                org.mockito.Mockito.spy(service);
        org.mockito.Mockito.doThrow(new IllegalStateException("first failure"))
                .when(spyService)
                .assignInNewTransaction(7L, ConsultationMatchType.NORMAL);
        org.mockito.Mockito.doReturn(true)
                .when(spyService)
                .assignInNewTransaction(8L, ConsultationMatchType.NORMAL);

        spyService.processDueAssignments();

        verify(spyService).assignInNewTransaction(
                7L, ConsultationMatchType.NORMAL);
        verify(spyService).assignInNewTransaction(
                8L, ConsultationMatchType.NORMAL);
    }

    @Test
    void scheduledScannerDelegatesToPerIntakeTransaction() throws NoSuchMethodException {
        Method scheduler = ConsultationMatchingService.class.getMethod("processDueAssignments");
        Method worker = ConsultationMatchingService.class.getMethod(
                "assignDueIntake", Long.class, ConsultationMatchType.class);

        assertNull(scheduler.getAnnotation(Transactional.class));
        Transactional transactional = worker.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Isolation.READ_COMMITTED, transactional.isolation());
    }

    private ConsultantIntakeEntity dueIntake(Long id, LocalDateTime confirmedAt) {
        return ConsultantIntakeEntity.builder()
                .id(id)
                .customerId(1L)
                .projectType("品牌设计")
                .industry("餐饮")
                .requirementDescription("需要完成完整品牌视觉升级")
                .budgetRange("1-2 万元")
                .projectCycle("4 周")
                .status(ConsultantIntakeStatus.READY_FOR_HANDOFF)
                .handoffConfirmedAt(confirmedAt)
                .nextMatchAt(confirmedAt.plusSeconds(3))
                .version(0)
                .createdAt(LocalDateTime.now(clock).minusMinutes(1))
                .updatedAt(LocalDateTime.now(clock))
                .build();
    }

    private void addDesigner(
            Long id,
            boolean enabled,
            DesignerAvailabilityStatus availability,
            boolean onDuty,
            int softCapacity,
            int hardCapacity,
            List<String> specialties,
            LocalDateTime lastAssignedAt) {
        users.put(id, new UserProfile(
                id, "设计师" + id, UserRole.DESIGNER, null, UserStatus.ENABLED));
        profiles.add(DesignerProfileEntity.builder()
                .designerId(id)
                .enabled(enabled)
                .autoMatchEnabled(true)
                .online(true)
                .availabilityStatus(availability)
                .specialties(specialties)
                .softCapacity(softCapacity)
                .hardCapacity(hardCapacity)
                .onDuty(onDuty)
                .lastAssignedAt(lastAssignedAt)
                .version(0)
                .build());
    }
}
