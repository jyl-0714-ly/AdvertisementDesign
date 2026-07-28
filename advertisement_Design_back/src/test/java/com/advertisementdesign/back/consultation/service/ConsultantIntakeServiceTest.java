package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.service.UnifiedConversationService;
import com.advertisementdesign.back.consultation.config.ConsultationMatchingProperties;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.model.ConsultantIntakeModels;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultantIntakeServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-28T02:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private IdentityService identityService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AuthService authService;
    @Mock
    private UnifiedConversationService unifiedConversationService;
    @Mock
    private ConsultationAcknowledgementService acknowledgementService;

    private final AtomicLong sequence = new AtomicLong();
    private Clock clock;
    private ConsultantIntakeService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZONE);
        service = new ConsultantIntakeService(
                consultationRepository,
                identityService,
                projectRepository,
                authService,
                unifiedConversationService,
                acknowledgementService,
                new ConsultationMatchingProperties(
                        Duration.ofSeconds(3), Duration.ofSeconds(60),
                        Duration.ofMillis(500)),
                clock);
        when(authService.currentUserProfile()).thenReturn(customer(1L));
    }

    @Test
    void completeSubmitQueuesHandoffWithoutImmediateAssignment() {
        stubIntakeCreation();
        ConsultantIntakeModels.ConsultantIntakeVO result = service.submit(submitRequest());

        assertEquals(ConsultantIntakeStatus.READY_FOR_HANDOFF, result.status());
        assertEquals(LocalDateTime.now(clock), capturedIntake().getHandoffConfirmedAt());
        assertEquals(LocalDateTime.now(clock).plusSeconds(3),
                capturedIntake().getNextMatchAt());
        assertNull(result.matchedDesigner());
        assertNull(result.humanChatId());
        assertNull(result.conversationId());
        assertEquals(List.of(), result.greetingMessages());
        verify(unifiedConversationService, never())
                .ensureConsultationConversation(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void completeDraftWaitsForExplicitHandoff() {
        stubIntakeCreation();
        ConsultantIntakeModels.ConsultantIntakeVO draft = service.createDraft(draftRequest(
                "品牌设计", "餐饮", "需要完成品牌视觉升级", "1-2 万元", "4 周"));

        assertEquals(ConsultantIntakeStatus.AGENT_COLLECTING, draft.status());
        assertNull(capturedIntake().getHandoffConfirmedAt());

        ConsultantIntakeEntity intake = capturedIntake();
        when(consultationRepository.findIntakeByIdForUpdate(intake.getId()))
                .thenReturn(Optional.of(intake));
        when(consultationRepository.confirmHandoff(
                intake.getId(), LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusSeconds(3)))
                .thenReturn(true);
        ConsultantIntakeModels.ConsultantIntakeVO handedOff = service.handoff(intake.getId());

        assertEquals(ConsultantIntakeStatus.READY_FOR_HANDOFF, handedOff.status());
        assertEquals(LocalDateTime.now(clock), intake.getHandoffConfirmedAt());
        assertEquals(LocalDateTime.now(clock).plusSeconds(3), intake.getNextMatchAt());
        assertNull(handedOff.matchedDesigner());
        verify(unifiedConversationService, never())
                .ensureConsultationConversation(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void repeatedHandoffPreservesOriginalConfirmationTime() {
        ConsultantIntakeEntity intake = completeIntake(9L, 1L);
        LocalDateTime confirmedAt = LocalDateTime.now(clock).minusSeconds(1);
        intake.setHandoffConfirmedAt(confirmedAt);
        intake.setStatus(ConsultantIntakeStatus.READY_FOR_HANDOFF);
        when(consultationRepository.findIntakeByIdForUpdate(9L)).thenReturn(Optional.of(intake));

        service.handoff(9L);
        service.handoff(9L);

        assertEquals(confirmedAt, intake.getHandoffConfirmedAt());
        verify(unifiedConversationService, never())
                .ensureConsultationConversation(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void incompleteDraftCannotBeHandedOff() {
        ConsultantIntakeEntity intake = completeIntake(9L, 1L);
        intake.setIndustry(null);
        intake.setStatus(ConsultantIntakeStatus.AGENT_COLLECTING);
        when(consultationRepository.findIntakeByIdForUpdate(9L)).thenReturn(Optional.of(intake));

        ApiException exception = assertThrows(ApiException.class, () -> service.handoff(9L));

        assertEquals(400, exception.getCode());
        assertNull(intake.getHandoffConfirmedAt());
    }

    @Test
    void customerCannotHandoffAnotherCustomersIntake() {
        ConsultantIntakeEntity intake = completeIntake(9L, 2L);
        when(consultationRepository.findIntakeByIdForUpdate(9L)).thenReturn(Optional.of(intake));

        assertEquals(403, assertThrows(ApiException.class, () -> service.handoff(9L)).getCode());
    }

    @Test
    void submitRejectsDesignerAndDisabledCustomer() {
        when(authService.currentUserProfile()).thenReturn(
                new UserProfile(2L, "设计师", UserRole.DESIGNER, null, UserStatus.ENABLED));
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.submit(submitRequest())).getCode());

        when(authService.currentUserProfile()).thenReturn(
                new UserProfile(1L, "客户", UserRole.CUSTOMER, null, UserStatus.DISABLED));
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.submit(submitRequest())).getCode());
    }

    @Test
    void matchedDesignerAcceptDelegatesToIdempotentAcknowledgementService() {
        UserProfile designer = new UserProfile(
                2L, "设计师", UserRole.DESIGNER, null, UserStatus.ENABLED);
        when(authService.currentUserProfile()).thenReturn(designer);
        when(identityService.findById(1L)).thenReturn(Optional.of(customer(1L)));
        ConsultantIntakeEntity intake = completeIntake(9L, 1L);
        intake.setMatchedDesignerId(2L);
        intake.setStatus(ConsultantIntakeStatus.MATCHED);
        when(consultationRepository.findIntakeByIdForUpdate(9L)).thenReturn(Optional.of(intake));
        when(consultationRepository.findIntakeById(9L)).thenReturn(Optional.of(intake));
        when(acknowledgementService.acknowledge(9L, 2L)).thenAnswer(invocation -> {
            intake.setStatus(ConsultantIntakeStatus.ACCEPTED);
            intake.setDesignerAcknowledgedAt(LocalDateTime.now(clock));
            return true;
        });

        ConsultantIntakeModels.DesignerReceptionVO accepted = service.accept(9L);

        assertEquals(ConsultantIntakeStatus.ACCEPTED, accepted.status());
        verify(acknowledgementService).acknowledge(9L, 2L);
    }

    private void stubIntakeCreation() {
        when(consultationRepository.saveIntake(any())).thenAnswer(invocation -> {
            ConsultantIntakeEntity intake = invocation.getArgument(0);
            if (intake.getId() == null) {
                intake.setId(sequence.incrementAndGet());
                intake.setCreatedAt(LocalDateTime.now(clock));
            }
            intake.setUpdatedAt(LocalDateTime.now(clock));
            return intake;
        });
        when(unifiedConversationService
                .findConversationIdByConsultantIntakeId(anyLong()))
                .thenReturn(null);
    }

    private ConsultantIntakeEntity capturedIntake() {
        try {
            org.mockito.ArgumentCaptor<ConsultantIntakeEntity> captor =
                    org.mockito.ArgumentCaptor.forClass(ConsultantIntakeEntity.class);
            verify(consultationRepository, org.mockito.Mockito.atLeastOnce()).saveIntake(captor.capture());
            return captor.getValue();
        } catch (AssertionError error) {
            throw error;
        }
    }

    private ConsultantIntakeEntity completeIntake(Long id, Long customerId) {
        return ConsultantIntakeEntity.builder()
                .id(id)
                .customerId(customerId)
                .projectType("品牌设计")
                .industry("餐饮")
                .requirementDescription("需要完成品牌视觉升级")
                .budgetRange("1-2 万元")
                .projectCycle("4 周")
                .status(ConsultantIntakeStatus.READY_FOR_HANDOFF)
                .createdAt(LocalDateTime.now(clock))
                .updatedAt(LocalDateTime.now(clock))
                .build();
    }

    private ConsultantIntakeModels.SaveConsultantIntakeDraftRequest draftRequest(
            String projectType,
            String industry,
            String description,
            String budget,
            String cycle) {
        return new ConsultantIntakeModels.SaveConsultantIntakeDraftRequest(
                projectType, industry, description, budget, cycle);
    }

    private ConsultantIntakeModels.SubmitConsultantIntakeRequest submitRequest() {
        return new ConsultantIntakeModels.SubmitConsultantIntakeRequest(
                "品牌设计", "餐饮", "需要完成品牌视觉升级", "1-2 万元", "4 周");
    }

    private UserProfile customer(Long id) {
        return new UserProfile(id, "客户", UserRole.CUSTOMER, null, UserStatus.ENABLED);
    }
}
