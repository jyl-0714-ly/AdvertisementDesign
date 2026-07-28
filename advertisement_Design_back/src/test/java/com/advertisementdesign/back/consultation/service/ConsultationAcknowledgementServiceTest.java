package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.consultation.config.ConsultationMatchingProperties;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.ConsultationDesignerMatchEntity;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchType;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationAcknowledgementServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Instant NOW = Instant.parse("2026-07-28T02:01:00Z");

    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private ConsultationMatchingService matchingService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private Clock clock;
    private ConsultationAcknowledgementService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZONE);
        service = new ConsultationAcknowledgementService(
                consultationRepository,
                matchingService,
                new ConsultationMatchingProperties(
                        Duration.ofSeconds(3), Duration.ofSeconds(60),
                        Duration.ofMillis(500)),
                clock,
                transactionManager);
        org.mockito.Mockito.lenient().when(consultationRepository
                        .acknowledgePendingMatch(any(), any(), any()))
                .thenReturn(true);
        org.mockito.Mockito.lenient().when(consultationRepository
                        .acceptMatchedIntake(any(), any(), any()))
                .thenReturn(true);
        org.mockito.Mockito.lenient().when(consultationRepository
                        .expirePendingMatch(any(), any(), any()))
                .thenReturn(true);
        org.mockito.Mockito.lenient().when(consultationRepository
                        .releaseExpiredAssignment(any(), any(), any(), any()))
                .thenReturn(true);
    }

    @Test
    void explicitAcknowledgementUpdatesMatchAndIntakeAndIsIdempotent() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(
                LocalDateTime.now(clock).plusNanos(1_000_000));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L)).thenReturn(Optional.of(match));

        assertTrue(service.acknowledge(7L, 2L));
        assertFalse(service.acknowledge(7L, 2L));

        assertEquals(ConsultationMatchStatus.ACKNOWLEDGED, match.getStatus());
        assertEquals(LocalDateTime.now(clock), match.getAcknowledgedAt());
        assertEquals(ConsultantIntakeStatus.ACCEPTED, intake.getStatus());
        assertEquals(LocalDateTime.now(clock), intake.getDesignerAcknowledgedAt());
    }

    @Test
    void acknowledgementAtOrAfterDeadlineIsNotAccepted() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(LocalDateTime.now(clock));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L)).thenReturn(Optional.of(match));

        assertFalse(service.acknowledge(7L, 2L));

        assertEquals(ConsultationMatchStatus.PENDING_ACK, match.getStatus());
        assertEquals(ConsultantIntakeStatus.MATCHED, intake.getStatus());
        verify(consultationRepository, never()).saveDesignerMatch(any());
    }

    @Test
    void acknowledgementOneMillisecondAfterDeadlineIsNotAccepted() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(
                LocalDateTime.now(clock).minusNanos(1_000_000));
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L))
                .thenReturn(Optional.of(match));

        assertFalse(service.acknowledge(7L, 2L));

        assertEquals(ConsultationMatchStatus.PENDING_ACK, match.getStatus());
        verify(consultationRepository, never()).saveDesignerMatch(any());
    }

    @Test
    void concurrentAcknowledgementLosesConditionalUpdateWithoutChangingState() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(
                LocalDateTime.now(clock).plusSeconds(1));
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L))
                .thenReturn(Optional.of(match));
        when(consultationRepository.acknowledgePendingMatch(
                10L, 2L, LocalDateTime.now(clock)))
                .thenReturn(false);

        assertFalse(service.acknowledge(7L, 2L));

        assertEquals(ConsultationMatchStatus.PENDING_ACK, match.getStatus());
        assertEquals(ConsultantIntakeStatus.MATCHED, intake.getStatus());
        verify(consultationRepository, never())
                .acceptMatchedIntake(any(), any(), any());
    }

    @Test
    void nonCurrentDesignerCannotAcknowledge() {
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(matchedIntake()));

        assertEquals(403, assertThrows(ApiException.class,
                () -> service.acknowledge(7L, 3L)).getCode());
    }

    @Test
    void expiryIsUnavailableBeforeBoundary() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(
                LocalDateTime.now(clock).plusNanos(1_000_000));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L)).thenReturn(Optional.of(match));

        assertFalse(service.expireAndRetry(7L));

        assertEquals(ConsultationMatchStatus.PENDING_ACK, match.getStatus());
        verify(matchingService, never()).assignDueIntake(any(), any());
    }

    @Test
    void expiryAtSixtySecondBoundaryMarksAttemptAndRetries() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(LocalDateTime.now(clock));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L)).thenReturn(Optional.of(match));
        when(matchingService.assignDueIntake(7L, ConsultationMatchType.RETRY)).thenReturn(true);

        assertTrue(service.expireAndRetry(7L));

        assertEquals(ConsultationMatchStatus.EXPIRED, match.getStatus());
        assertEquals(LocalDateTime.now(clock), match.getExpiredAt());
        assertEquals(ConsultantIntakeStatus.READY_FOR_HANDOFF, intake.getStatus());
        assertNull(intake.getMatchedDesignerId());
        assertEquals(LocalDateTime.now(clock).minusSeconds(3), intake.getHandoffConfirmedAt());
        verify(matchingService).assignDueIntake(7L, ConsultationMatchType.RETRY);
    }

    @Test
    void expiryOneMillisecondAfterBoundaryMarksAttemptAndRetries() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(
                LocalDateTime.now(clock).minusNanos(1_000_000));
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L))
                .thenReturn(Optional.of(match));
        when(matchingService.assignDueIntake(7L, ConsultationMatchType.RETRY))
                .thenReturn(true);

        assertTrue(service.expireAndRetry(7L));

        assertEquals(ConsultationMatchStatus.EXPIRED, match.getStatus());
        verify(matchingService).assignDueIntake(7L, ConsultationMatchType.RETRY);
    }

    @Test
    void repeatedExpiryProcessingRetriesOnlyOnce() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(LocalDateTime.now(clock));
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L))
                .thenReturn(Optional.of(match), Optional.empty());
        when(matchingService.assignDueIntake(7L, ConsultationMatchType.RETRY))
                .thenReturn(true);

        assertTrue(service.expireAndRetry(7L));
        assertFalse(service.expireAndRetry(7L));

        verify(consultationRepository).expirePendingMatch(
                10L, 2L, LocalDateTime.now(clock));
        verify(consultationRepository).releaseExpiredAssignment(
                7L,
                2L,
                LocalDateTime.now(clock),
                LocalDateTime.now(clock).minusSeconds(3));
        verify(matchingService).assignDueIntake(7L, ConsultationMatchType.RETRY);
    }

    @Test
    void humanDesignerMessageUsesSameAcknowledgementPath() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(
                LocalDateTime.now(clock).plusSeconds(1));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L)).thenReturn(Optional.of(match));

        service.acknowledgeHumanDesignerMessage(7L, 2L);

        assertEquals(ConsultationMatchStatus.ACKNOWLEDGED, match.getStatus());
        assertEquals(ConsultantIntakeStatus.ACCEPTED, intake.getStatus());
    }

    @Test
    void expiredHumanMessageIsRejectedBeforePersistence() {
        ConsultantIntakeEntity intake = matchedIntake();
        ConsultationDesignerMatchEntity match = pendingMatch(LocalDateTime.now(clock));
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake));
        when(consultationRepository.findPendingMatchForUpdate(7L))
                .thenReturn(Optional.of(match));

        assertEquals(403, assertThrows(ApiException.class,
                () -> service.acknowledgeHumanDesignerMessage(7L, 2L)).getCode());

        verify(consultationRepository, never())
                .acknowledgePendingMatch(any(), any(), any());
        verify(consultationRepository, never())
                .acceptMatchedIntake(any(), any(), any());
    }

    @Test
    void expiryScannerContinuesAfterOneMatchFails() {
        ConsultationDesignerMatchEntity first = pendingMatch(
                LocalDateTime.now(clock));
        first.setConsultantIntakeId(7L);
        ConsultationDesignerMatchEntity second = pendingMatch(
                LocalDateTime.now(clock));
        second.setConsultantIntakeId(8L);
        when(consultationRepository.listExpiredPendingMatches(
                LocalDateTime.now(clock), 50))
                .thenReturn(List.of(first, second));
        ConsultationAcknowledgementService spyService =
                org.mockito.Mockito.spy(service);
        org.mockito.Mockito.doThrow(new IllegalStateException("first failure"))
                .when(spyService).expireInNewTransaction(7L);
        org.mockito.Mockito.doReturn(true)
                .when(spyService).expireInNewTransaction(8L);

        spyService.processExpiredAcknowledgements();

        verify(spyService).expireInNewTransaction(7L);
        verify(spyService).expireInNewTransaction(8L);
    }

    @Test
    void scheduledExpiryScannerDelegatesToPerMatchTransaction()
            throws NoSuchMethodException {
        Method scheduler = ConsultationAcknowledgementService.class
                .getMethod("processExpiredAcknowledgements");
        Method worker = ConsultationAcknowledgementService.class
                .getMethod("expireAndRetry", Long.class);

        assertNull(scheduler.getAnnotation(Transactional.class));
        assertNotNull(worker.getAnnotation(Transactional.class));
    }

    private ConsultantIntakeEntity matchedIntake() {
        return ConsultantIntakeEntity.builder()
                .id(7L)
                .customerId(1L)
                .matchedDesignerId(2L)
                .initialDesignerId(2L)
                .status(ConsultantIntakeStatus.MATCHED)
                .designerAssignedAt(LocalDateTime.now(clock).minusSeconds(60))
                .matchAttemptCount(1)
                .version(0)
                .createdAt(LocalDateTime.now(clock).minusMinutes(2))
                .updatedAt(LocalDateTime.now(clock))
                .build();
    }

    private ConsultationDesignerMatchEntity pendingMatch(LocalDateTime expiresAt) {
        return ConsultationDesignerMatchEntity.builder()
                .id(10L)
                .consultantIntakeId(7L)
                .designerId(2L)
                .attemptNo(1)
                .matchType(ConsultationMatchType.NORMAL)
                .status(ConsultationMatchStatus.PENDING_ACK)
                .assignedAt(LocalDateTime.now(clock).minusSeconds(60))
                .expiresAt(expiresAt)
                .version(0)
                .build();
    }
}
