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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationMatchingService {
    private static final int MATCH_BATCH_SIZE = 50;
    private static final Duration NO_CANDIDATE_RETRY_DELAY = Duration.ofSeconds(5);
    private static final String WAITING_GREETING = "您的需求已整理完成，请稍等。";

    private final ConsultationRepository consultationRepository;
    private final ProjectRepository projectRepository;
    private final IdentityService identityService;
    private final UnifiedConversationService unifiedConversationService;
    private final ConsultationMatchingProperties properties;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelayString = "${app.consultation.scheduler-delay:500}")
    public void processDueAssignments() {
        LocalDateTime dueAt = now();
        consultationRepository.listDueHandoffIntakes(dueAt, MATCH_BATCH_SIZE)
                .forEach(intake -> processDueAssignmentSafely(intake.getId()));
    }

    void processDueAssignmentSafely(Long intakeId) {
        try {
            assignInNewTransaction(intakeId, ConsultationMatchType.NORMAL);
        } catch (RuntimeException exception) {
            log.error("自动匹配需求单失败，consultantIntakeId={}，exceptionType={}",
                    intakeId, exception.getClass().getSimpleName());
        }
    }

    boolean assignInNewTransaction(
            Long intakeId,
            ConsultationMatchType requestedType) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return Boolean.TRUE.equals(transaction.execute(status ->
                assignDueIntake(intakeId, requestedType)));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean assignDueIntake(Long intakeId, ConsultationMatchType requestedType) {
        ConsultantIntakeEntity intake = consultationRepository.findIntakeByIdForUpdate(intakeId)
                .orElse(null);
        LocalDateTime now = now();
        if (intake == null
                || intake.getStatus() != ConsultantIntakeStatus.READY_FOR_HANDOFF
                || intake.getHandoffConfirmedAt() == null
                || intake.getNextMatchAt() == null
                || intake.getNextMatchAt().isAfter(now)
                || consultationRepository.hasPendingMatch(intakeId)) {
            return false;
        }
        List<Long> failedDesignerIds = consultationRepository.listFailedDesignerIds(intakeId);
        boolean requireOnDuty = true;
        Candidate winner = selectCandidate(intake, failedDesignerIds, requireOnDuty);
        ConsultationMatchType matchType = requestedType;
        if (winner == null) {
            requireOnDuty = false;
            winner = selectCandidate(intake, failedDesignerIds, requireOnDuty);
            if (requestedType == ConsultationMatchType.NORMAL) {
                matchType = ConsultationMatchType.ON_DUTY_FALLBACK;
            }
        }
        if (winner == null) {
            return deferUnassignedIntake(intake, requestedType);
        }
        DesignerProfileEntity lockedProfile = consultationRepository
                .findDesignerProfileForUpdate(winner.profile().getDesignerId())
                .orElse(null);
        if (lockedProfile == null
                || !isHardEligible(lockedProfile, failedDesignerIds, requireOnDuty)) {
            return deferUnassignedIntake(intake, requestedType);
        }
        long lockedWorkload = activeWorkload(lockedProfile.getDesignerId());
        if (lockedWorkload >= hardCapacity(lockedProfile)) {
            return deferUnassignedIntake(intake, requestedType);
        }

        UserProfile designer = identityService.findById(lockedProfile.getDesignerId()).orElse(null);
        if (!isEnabledDesigner(designer)) {
            return deferUnassignedIntake(intake, requestedType);
        }
        int attemptNo = Objects.requireNonNullElse(intake.getMatchAttemptCount(), 0) + 1;
        LocalDateTime expiresAt = now.plus(properties.acknowledgementTimeout());
        consultationRepository.saveDesignerMatch(ConsultationDesignerMatchEntity.builder()
                .consultantIntakeId(intake.getId())
                .designerId(designer.id())
                .attemptNo(attemptNo)
                .matchType(matchType)
                .status(ConsultationMatchStatus.PENDING_ACK)
                .activityScore(winner.activityScore())
                .workloadScore(winner.workloadScore())
                .specialtyScore(winner.specialtyScore())
                .fairnessScore(winner.fairnessScore())
                .totalScore(winner.totalScore())
                .requirementConfidence(winner.requirementConfidence())
                .scoreDetail(winner.scoreDetail())
                .assignedAt(now)
                .expiresAt(expiresAt)
                .version(0)
                .build());

        if (intake.getInitialDesignerId() == null) {
            intake.setInitialDesignerId(designer.id());
        }
        intake.setMatchedDesignerId(designer.id());
        intake.setDesignerAssignedAt(now);
        intake.setDesignerAcknowledgedAt(null);
        intake.setMatchAttemptCount(attemptNo);
        intake.setNextMatchAt(null);
        intake.setStatus(ConsultantIntakeStatus.MATCHED);
        if (intake.getHumanChatId() == null) {
            intake.setHumanChatId("consultant-" + UUID.randomUUID());
        }
        List<String> greetings = List.of(
                "您好，我是" + designer.nickname() + "，接下来由我与您确认需求细节并推进设计方案。",
                WAITING_GREETING);
        intake.setGreetingMessages(greetings);
        consultationRepository.saveIntake(intake);

        lockedProfile.setLastAssignedAt(now);
        consultationRepository.saveDesignerProfile(lockedProfile);
        unifiedConversationService.ensureConsultationConversation(
                intake.getId(), intake.getCustomerId(), designer.id(), greetings);
        return true;
    }

    private boolean deferUnassignedIntake(
            ConsultantIntakeEntity intake,
            ConsultationMatchType matchType) {
        LocalDateTime deferredAt = now();
        int attemptNo = Objects.requireNonNullElse(
                intake.getMatchAttemptCount(), 0) + 1;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("reason", "NO_ELIGIBLE_DESIGNER");
        detail.put("retryDelaySeconds",
                NO_CANDIDATE_RETRY_DELAY.toSeconds());
        consultationRepository.saveDesignerMatch(
                ConsultationDesignerMatchEntity.builder()
                        .consultantIntakeId(intake.getId())
                        .attemptNo(attemptNo)
                        .matchType(matchType)
                        .status(ConsultationMatchStatus.NO_CANDIDATE)
                        .scoreDetail(detail)
                        .cancelledAt(deferredAt)
                        .version(0)
                        .build());
        intake.setMatchAttemptCount(attemptNo);
        intake.setNextMatchAt(
                deferredAt.plus(NO_CANDIDATE_RETRY_DELAY));
        consultationRepository.saveIntake(intake);
        return false;
    }

    private Candidate selectCandidate(
            ConsultantIntakeEntity intake,
            List<Long> failedDesignerIds,
            boolean requireOnDuty) {
        return consultationRepository.listEnabledDesignerProfiles().stream()
                .filter(profile -> isHardEligible(profile, failedDesignerIds, requireOnDuty))
                .filter(profile -> isEnabledDesigner(
                        identityService.findById(profile.getDesignerId()).orElse(null)))
                .map(profile -> score(profile, intake))
                .max(Comparator.comparing(Candidate::totalScore)
                        .thenComparing(candidate -> candidate.profile().getDesignerId(),
                                Comparator.reverseOrder()))
                .orElse(null);
    }

    private boolean isHardEligible(
            DesignerProfileEntity profile,
            List<Long> failedDesignerIds,
            boolean requireOnDuty) {
        if (!Boolean.TRUE.equals(profile.getEnabled())
                || !Boolean.TRUE.equals(profile.getAutoMatchEnabled())
                || profile.getAvailabilityStatus() == DesignerAvailabilityStatus.ON_LEAVE
                || profile.getAvailabilityStatus() == DesignerAvailabilityStatus.STOPPED
                || failedDesignerIds.contains(profile.getDesignerId())) {
            return false;
        }
        if (requireOnDuty && !Boolean.TRUE.equals(profile.getOnDuty())) {
            return false;
        }
        return activeWorkload(profile.getDesignerId()) < hardCapacity(profile);
    }

    private Candidate score(DesignerProfileEntity profile, ConsultantIntakeEntity intake) {
        long workload = activeWorkload(profile.getDesignerId());
        int hardCapacity = hardCapacity(profile);
        int scoringCapacity = scoringCapacity(profile, hardCapacity);
        BigDecimal activity = scoreActivity(profile);
        BigDecimal workloadScore = scoreWorkload(workload, scoringCapacity);
        SpecialtyResult specialty = scoreSpecialty(profile, intake);
        BigDecimal fairness = scoreFairness(profile);
        BigDecimal total = activity.multiply(new BigDecimal("0.35"))
                .add(workloadScore.multiply(new BigDecimal("0.30")))
                .add(specialty.score().multiply(new BigDecimal("0.20")))
                .add(fairness.multiply(new BigDecimal("0.15")))
                .setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("activeWorkload", workload);
        detail.put("softCapacity", scoringCapacity);
        detail.put("hardCapacity", hardCapacity);
        detail.put("online", Boolean.TRUE.equals(profile.getOnline()));
        detail.put("onDuty", Boolean.TRUE.equals(profile.getOnDuty()));
        detail.put("specialtyMatched", specialty.matched());
        detail.put("specialtyNeutral", specialty.neutral());
        return new Candidate(profile, activity, workloadScore, specialty.score(), fairness,
                total, specialty.confidence(), detail);
    }

    private BigDecimal scoreActivity(DesignerProfileEntity profile) {
        if (Boolean.TRUE.equals(profile.getOnline())) {
            return new BigDecimal("100");
        }
        if (profile.getLastActiveAt() == null) {
            return new BigDecimal("30");
        }
        long minutes = Math.max(0, Duration.between(profile.getLastActiveAt(), now()).toMinutes());
        if (minutes <= 5) {
            return new BigDecimal("90");
        }
        if (minutes <= 30) {
            return new BigDecimal("70");
        }
        if (minutes <= 120) {
            return new BigDecimal("50");
        }
        return new BigDecimal("30");
    }

    private BigDecimal scoreWorkload(long workload, int hardCapacity) {
        if (hardCapacity <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal utilization = BigDecimal.valueOf(workload)
                .divide(BigDecimal.valueOf(hardCapacity), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100)
                .multiply(BigDecimal.ONE.subtract(utilization).max(BigDecimal.ZERO))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private SpecialtyResult scoreSpecialty(
            DesignerProfileEntity profile,
            ConsultantIntakeEntity intake) {
        String projectType = normalize(intake.getProjectType());
        String industry = normalize(intake.getIndustry());
        String description = normalize(intake.getRequirementDescription());
        boolean ambiguous = projectType.isBlank() || "其他".equals(projectType)
                || industry.isBlank() || description.length() < 8;
        if (ambiguous) {
            return new SpecialtyResult(new BigDecimal("50"), new BigDecimal("30"), false, true);
        }
        boolean matched = profile.getSpecialties() != null && profile.getSpecialties().stream()
                .map(this::normalize)
                .filter(specialty -> !specialty.isBlank())
                .anyMatch(specialty -> specialty.equals(projectType)
                        || specialty.equals(industry)
                        || description.contains(specialty));
        return new SpecialtyResult(
                matched ? new BigDecimal("100") : new BigDecimal("40"),
                new BigDecimal("80"), matched, false);
    }

    private BigDecimal scoreFairness(DesignerProfileEntity profile) {
        if (profile.getLastAssignedAt() == null) {
            return new BigDecimal("100");
        }
        long minutes = Math.max(0, Duration.between(profile.getLastAssignedAt(), now()).toMinutes());
        return BigDecimal.valueOf(Math.min(100, 20 + minutes));
    }

    private long activeWorkload(Long designerId) {
        return projectRepository.countInProgressProjectsByDesigner(designerId)
                + consultationRepository.countActiveIntakesByDesigner(designerId);
    }

    private int hardCapacity(DesignerProfileEntity profile) {
        return profile.getHardCapacity() == null || profile.getHardCapacity() <= 0
                ? 1 : profile.getHardCapacity();
    }

    private int scoringCapacity(DesignerProfileEntity profile, int hardCapacity) {
        if (profile.getSoftCapacity() == null || profile.getSoftCapacity() <= 0) {
            return hardCapacity;
        }
        return Math.min(profile.getSoftCapacity(), hardCapacity);
    }

    private boolean isEnabledDesigner(UserProfile user) {
        return user != null && user.role() == UserRole.DESIGNER
                && user.status() == UserStatus.ENABLED;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private record SpecialtyResult(
            BigDecimal score,
            BigDecimal confidence,
            boolean matched,
            boolean neutral) {
    }

    private record Candidate(
            DesignerProfileEntity profile,
            BigDecimal activityScore,
            BigDecimal workloadScore,
            BigDecimal specialtyScore,
            BigDecimal fairnessScore,
            BigDecimal totalScore,
            BigDecimal requirementConfidence,
            Map<String, Object> scoreDetail) {
    }
}
