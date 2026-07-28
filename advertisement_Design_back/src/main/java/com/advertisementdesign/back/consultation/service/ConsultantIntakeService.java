package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.consultation.entity.ConsultantHumanMessageEntity;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.DesignerProfileEntity;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.model.ConsultantIntakeModels;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultantIntakeService {
    private static final String DESIGNER_HANDOFF_GREETING = "您的需求已整理完成，请稍等。";

    private final ConsultationRepository consultationRepository;
    private final IdentityService identityService;
    private final ProjectRepository projectRepository;
    private final AuthService authService;

    /**
     * Legacy compatibility endpoint: a complete submission still matches immediately.
     */
    @Transactional
    public ConsultantIntakeModels.ConsultantIntakeVO submit(
            ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        UserProfile customer = currentCustomer();
        ConsultantIntakeEntity intake = ConsultantIntakeEntity.builder()
                .customerId(customer.id())
                .projectType(clean(request.projectType()))
                .industry(clean(request.industry()))
                .requirementDescription(clean(request.requirementDescription()))
                .budgetRange(clean(request.budgetRange()))
                .projectCycle(clean(request.projectCycle()))
                .status(ConsultantIntakeStatus.READY_FOR_HANDOFF)
                .build();
        consultationRepository.saveIntake(intake);
        return performHandoff(intake);
    }

    @Transactional
    public ConsultantIntakeModels.ConsultantIntakeVO createDraft(
            ConsultantIntakeModels.SaveConsultantIntakeDraftRequest request) {
        UserProfile customer = currentCustomer();
        ConsultantIntakeEntity intake = ConsultantIntakeEntity.builder()
                .customerId(customer.id())
                .build();
        applyDraft(intake, request);
        return toCustomerVO(consultationRepository.saveIntake(intake));
    }

    @Transactional
    public ConsultantIntakeModels.ConsultantIntakeVO updateDraft(
            Long intakeId,
            ConsultantIntakeModels.SaveConsultantIntakeDraftRequest request) {
        ConsultantIntakeEntity intake = accessibleCustomerIntake(intakeId, currentCustomer());
        if (intake.getStatus() == ConsultantIntakeStatus.MATCHED
                || intake.getStatus() == ConsultantIntakeStatus.ACCEPTED) {
            return toCustomerVO(intake);
        }
        applyDraft(intake, request);
        return toCustomerVO(consultationRepository.saveIntake(intake));
    }

    public ConsultantIntakeModels.ConsultantIntakeVO getCurrentCustomerIntake() {
        UserProfile customer = currentCustomer();
        ConsultantIntakeEntity intake = consultationRepository.findCurrentIntakeByCustomer(customer.id())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        return toCustomerVO(intake);
    }

    @Transactional
    public ConsultantIntakeModels.ConsultantIntakeVO handoff(Long intakeId) {
        ConsultantIntakeEntity intake = accessibleCustomerIntakeForUpdate(intakeId, currentCustomer());
        if (intake.getStatus() == ConsultantIntakeStatus.MATCHED
                || intake.getStatus() == ConsultantIntakeStatus.ACCEPTED) {
            return toCustomerVO(intake);
        }
        if (!isComplete(intake)) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "需求信息未填写完整，无法转接人工设计师");
        }
        intake.setStatus(ConsultantIntakeStatus.READY_FOR_HANDOFF);
        return performHandoff(intake);
    }

    public List<ConsultantIntakeModels.DesignerReceptionVO> listDesignerReceptions() {
        UserProfile designer = authService.currentUserProfile();
        ensureDesigner(designer);
        return consultationRepository.listIntakesByDesigner(designer.id()).stream()
                .map(this::toDesignerReceptionVO)
                .toList();
    }

    public ConsultantIntakeModels.DesignerReceptionVO getDesignerReception(Long intakeId) {
        UserProfile designer = authService.currentUserProfile();
        ensureDesigner(designer);
        return toDesignerReceptionVO(accessibleDesignerIntake(intakeId, designer));
    }

    @Transactional
    public ConsultantIntakeModels.DesignerReceptionVO accept(Long intakeId) {
        UserProfile designer = authService.currentUserProfile();
        ensureDesigner(designer);
        ConsultantIntakeEntity intake = accessibleDesignerIntake(intakeId, designer);
        if (intake.getStatus() == ConsultantIntakeStatus.MATCHED) {
            intake.setStatus(ConsultantIntakeStatus.ACCEPTED);
            consultationRepository.saveIntake(intake);
        }
        return toDesignerReceptionVO(intake);
    }

    private ConsultantIntakeModels.ConsultantIntakeVO performHandoff(ConsultantIntakeEntity intake) {
        DesignerProfileEntity designerProfile = matchDesigner(intake);
        UserProfile designer = findUser(designerProfile.getDesignerId());
        if (designer.role() != UserRole.DESIGNER) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        List<String> greetings = List.of(
                "您好，我是" + designer.nickname() + "，接下来由我与您确认需求细节并推进设计方案。",
                DESIGNER_HANDOFF_GREETING
        );
        intake.setStatus(ConsultantIntakeStatus.MATCHED);
        intake.setMatchedDesignerId(designer.id());
        intake.setHumanChatId("consultant-" + UUID.randomUUID());
        intake.setGreetingMessages(greetings);
        consultationRepository.saveIntake(intake);
        greetings.forEach(content -> consultationRepository.saveHumanMessage(
                ConsultantHumanMessageEntity.builder()
                        .humanChatId(intake.getHumanChatId())
                        .senderId(designer.id())
                        .senderRole(MessageSenderRole.DESIGNER)
                        .content(content)
                        .build()));
        return toCustomerVO(intake, designerProfile, designer);
    }

    private void applyDraft(
            ConsultantIntakeEntity intake,
            ConsultantIntakeModels.SaveConsultantIntakeDraftRequest request) {
        intake.setProjectType(clean(request.projectType()));
        intake.setIndustry(clean(request.industry()));
        intake.setRequirementDescription(clean(request.requirementDescription()));
        intake.setBudgetRange(clean(request.budgetRange()));
        intake.setProjectCycle(clean(request.projectCycle()));
        intake.setStatus(isComplete(intake)
                ? ConsultantIntakeStatus.READY_FOR_HANDOFF
                : ConsultantIntakeStatus.AGENT_COLLECTING);
    }

    private ConsultantIntakeEntity accessibleCustomerIntake(Long intakeId, UserProfile customer) {
        ConsultantIntakeEntity intake = consultationRepository.findIntakeById(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!customer.id().equals(intake.getCustomerId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return intake;
    }

    private ConsultantIntakeEntity accessibleCustomerIntakeForUpdate(Long intakeId, UserProfile customer) {
        ConsultantIntakeEntity intake = consultationRepository.findIntakeByIdForUpdate(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!customer.id().equals(intake.getCustomerId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return intake;
    }

    private ConsultantIntakeEntity accessibleDesignerIntake(Long intakeId, UserProfile designer) {
        ConsultantIntakeEntity intake = consultationRepository.findIntakeById(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!designer.id().equals(intake.getMatchedDesignerId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return intake;
    }

    private ConsultantIntakeModels.ConsultantIntakeVO toCustomerVO(ConsultantIntakeEntity intake) {
        if (intake.getMatchedDesignerId() == null) {
            return toCustomerVO(intake, null, null);
        }
        UserProfile designer = findUser(intake.getMatchedDesignerId());
        DesignerProfileEntity profile = consultationRepository
                .findDesignerProfile(intake.getMatchedDesignerId()).orElse(null);
        return toCustomerVO(intake, profile, designer);
    }

    private ConsultantIntakeModels.ConsultantIntakeVO toCustomerVO(
            ConsultantIntakeEntity intake,
            DesignerProfileEntity profile,
            UserProfile designer) {
        ConsultantIntakeModels.MatchedDesignerVO matchedDesigner = designer == null ? null
                : new ConsultantIntakeModels.MatchedDesignerVO(
                        designer.id(),
                        designer.nickname(),
                        designer.avatar(),
                        profile != null && Boolean.TRUE.equals(profile.getOnline()),
                        profile == null || profile.getSpecialties() == null
                                ? List.of()
                                : List.copyOf(profile.getSpecialties()));
        return new ConsultantIntakeModels.ConsultantIntakeVO(
                intake.getId(),
                intake.getStatus(),
                intake.getProjectType(),
                intake.getIndustry(),
                intake.getRequirementDescription(),
                intake.getBudgetRange(),
                intake.getProjectCycle(),
                matchedDesigner,
                intake.getHumanChatId(),
                intake.getGreetingMessages() == null ? List.of() : List.copyOf(intake.getGreetingMessages()),
                intake.getCreatedAt().toString());
    }

    private ConsultantIntakeModels.DesignerReceptionVO toDesignerReceptionVO(ConsultantIntakeEntity intake) {
        UserProfile customer = findUser(intake.getCustomerId());
        DesignerProfileEntity profile = consultationRepository
                .findDesignerProfile(intake.getMatchedDesignerId()).orElse(null);
        boolean specialtyMatched = profile != null && profile.getSpecialties() != null
                && profile.getSpecialties().stream()
                .map(this::normalize)
                .anyMatch(specialty -> specialty.equals(normalize(intake.getProjectType()))
                        || specialty.equals(normalize(intake.getIndustry())));
        long activeWorkload = activeWorkload(intake.getMatchedDesignerId());
        int score = 78
                + (profile != null && Boolean.TRUE.equals(profile.getOnline()) ? 8 : 0)
                + (specialtyMatched ? 6 : 0)
                + (activeWorkload <= 1 ? 4 : 0);
        boolean online = profile != null && Boolean.TRUE.equals(profile.getOnline());
        String reason;
        if (specialtyMatched && online) {
            reason = "当前在线，且您的专业方向与该需求高度匹配";
        } else if (specialtyMatched) {
            reason = "您的专业方向与该需求高度匹配";
        } else if (online && activeWorkload <= 1) {
            reason = "当前在线且项目负载较低，适合及时接待";
        } else {
            reason = "当前项目负载适合接待该客户";
        }
        return new ConsultantIntakeModels.DesignerReceptionVO(
                intake.getId(), intake.getStatus(), customer.id(), customer.nickname(), customer.avatar(),
                intake.getProjectType(), intake.getIndustry(), intake.getRequirementDescription(),
                intake.getBudgetRange(), intake.getProjectCycle(), Math.min(score, 96), reason,
                intake.getHumanChatId(), intake.getCreatedAt().toString());
    }

    private DesignerProfileEntity matchDesigner(ConsultantIntakeEntity intake) {
        return consultationRepository.listEnabledDesignerProfiles().stream()
                .filter(profile -> {
                    UserProfile user = identityService.findById(profile.getDesignerId()).orElse(null);
                    return user != null && user.role() == UserRole.DESIGNER
                            && user.status() == UserStatus.ENABLED;
                })
                .min(Comparator
                        .comparing((DesignerProfileEntity profile) -> !Boolean.TRUE.equals(profile.getOnline()))
                        .thenComparingLong(profile -> activeWorkload(profile.getDesignerId()))
                        .thenComparing(profile -> !hasSpecialtyMatch(profile, intake))
                        .thenComparing(DesignerProfileEntity::getDesignerId))
                .orElseThrow(() -> new ApiException(1001, "当前暂无可匹配的设计师"));
    }

    private long activeWorkload(Long designerId) {
        return projectRepository.countInProgressProjectsByDesigner(designerId)
                + consultationRepository.countActiveIntakesByDesigner(designerId);
    }

    private boolean hasSpecialtyMatch(DesignerProfileEntity profile, ConsultantIntakeEntity intake) {
        String projectType = normalize(intake.getProjectType());
        String industry = normalize(intake.getIndustry());
        return profile.getSpecialties() != null && profile.getSpecialties().stream()
                .map(this::normalize)
                .anyMatch(specialty -> specialty.equals(projectType) || specialty.equals(industry));
    }

    private boolean isComplete(ConsultantIntakeEntity intake) {
        return !isBlank(intake.getProjectType())
                && !isBlank(intake.getIndustry())
                && !isBlank(intake.getRequirementDescription())
                && !isBlank(intake.getBudgetRange())
                && !isBlank(intake.getProjectCycle());
    }

    private UserProfile currentCustomer() {
        UserProfile customer = authService.currentUserProfile();
        ensureCustomer(customer);
        return customer;
    }

    private UserProfile findUser(Long userId) {
        return identityService.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureDesigner(UserProfile user) {
        if (user.role() != UserRole.DESIGNER || user.status() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private void ensureCustomer(UserProfile user) {
        if (user.role() != UserRole.CUSTOMER || user.status() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
