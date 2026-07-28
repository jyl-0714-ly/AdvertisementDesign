package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.service.DesignerMessageAcknowledgementPort;
import com.advertisementdesign.back.consultation.config.ConsultationMatchingProperties;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.ConsultationDesignerMatchEntity;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchType;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAcknowledgementService
        implements DesignerMessageAcknowledgementPort {
    private static final int EXPIRY_BATCH_SIZE = 50;

    private final ConsultationRepository consultationRepository;
    private final ConsultationMatchingService matchingService;
    private final ConsultationMatchingProperties properties;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public void acknowledgeHumanDesignerMessage(
            Long consultantIntakeId,
            Long designerId) {
        acknowledge(consultantIntakeId, designerId, true);
    }

    @Transactional
    public boolean acknowledge(Long intakeId, Long designerId) {
        return acknowledge(intakeId, designerId, false);
    }

    private boolean acknowledge(
            Long intakeId,
            Long designerId,
            boolean requireActiveAssignment) {
        ConsultantIntakeEntity intake = consultationRepository.findIntakeByIdForUpdate(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!Objects.equals(intake.getMatchedDesignerId(), designerId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (intake.getDesignerAcknowledgedAt() != null
                || intake.getStatus() == ConsultantIntakeStatus.ACCEPTED) {
            return false;
        }
        if (intake.getStatus() != ConsultantIntakeStatus.MATCHED) {
            if (requireActiveAssignment) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
            return false;
        }
        ConsultationDesignerMatchEntity match = consultationRepository
                .findPendingMatchForUpdate(intakeId)
                .orElse(null);
        if (match == null || !Objects.equals(match.getDesignerId(), designerId)
                || match.getExpiresAt() == null
                || !match.getExpiresAt().isAfter(now())) {
            if (requireActiveAssignment) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
            return false;
        }
        LocalDateTime acknowledgedAt = now();
        if (!consultationRepository.acknowledgePendingMatch(
                match.getId(), designerId, acknowledgedAt)) {
            if (requireActiveAssignment) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
            return false;
        }
        if (!consultationRepository.acceptMatchedIntake(
                intakeId, designerId, acknowledgedAt)) {
            throw new IllegalStateException("咨询确认状态并发冲突");
        }
        match.setStatus(ConsultationMatchStatus.ACKNOWLEDGED);
        match.setAcknowledgedAt(acknowledgedAt);
        intake.setDesignerAcknowledgedAt(acknowledgedAt);
        intake.setStatus(ConsultantIntakeStatus.ACCEPTED);
        return true;
    }

    @Scheduled(fixedDelayString = "${app.consultation.scheduler-delay:500}")
    public void processExpiredAcknowledgements() {
        consultationRepository.listExpiredPendingMatches(now(), EXPIRY_BATCH_SIZE)
                .forEach(match -> processExpiredAcknowledgementSafely(
                        match.getConsultantIntakeId()));
    }

    void processExpiredAcknowledgementSafely(Long intakeId) {
        try {
            expireInNewTransaction(intakeId);
        } catch (RuntimeException exception) {
            log.error("处理设计师确认超时失败，consultantIntakeId={}，exceptionType={}",
                    intakeId, exception.getClass().getSimpleName());
        }
    }

    boolean expireInNewTransaction(Long intakeId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return Boolean.TRUE.equals(transaction.execute(status ->
                expireAndRetry(intakeId)));
    }

    @Transactional
    public boolean expireAndRetry(Long intakeId) {
        ConsultantIntakeEntity intake = consultationRepository.findIntakeByIdForUpdate(intakeId)
                .orElse(null);
        ConsultationDesignerMatchEntity match = consultationRepository
                .findPendingMatchForUpdate(intakeId)
                .orElse(null);
        LocalDateTime now = now();
        if (intake == null || match == null
                || intake.getStatus() != ConsultantIntakeStatus.MATCHED
                || intake.getDesignerAcknowledgedAt() != null
                || match.getExpiresAt() == null
                || match.getExpiresAt().isAfter(now)
                || !Objects.equals(intake.getMatchedDesignerId(), match.getDesignerId())) {
            return false;
        }
        if (!consultationRepository.expirePendingMatch(
                match.getId(), match.getDesignerId(), now)) {
            return false;
        }
        LocalDateTime handoffConfirmedAt =
                now.minus(properties.matchingDelay());
        if (!consultationRepository.releaseExpiredAssignment(
                intakeId,
                match.getDesignerId(),
                now,
                handoffConfirmedAt)) {
            throw new IllegalStateException("咨询超时释放状态并发冲突");
        }
        match.setStatus(ConsultationMatchStatus.EXPIRED);
        match.setExpiredAt(now);
        intake.setStatus(ConsultantIntakeStatus.READY_FOR_HANDOFF);
        intake.setMatchedDesignerId(null);
        intake.setDesignerAssignedAt(null);
        intake.setDesignerAcknowledgedAt(null);
        intake.setHandoffConfirmedAt(handoffConfirmedAt);
        intake.setNextMatchAt(now);
        return matchingService.assignDueIntake(intakeId, ConsultationMatchType.RETRY);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
