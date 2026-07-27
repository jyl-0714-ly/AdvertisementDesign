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

    @Transactional
    public ConsultantIntakeModels.ConsultantIntakeVO submit(
            ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        UserProfile customer = authService.currentUserProfile();
        ensureCustomer(customer);

        DesignerProfileEntity designerProfile = matchDesigner(request);
        UserProfile designer = findUser(designerProfile.getDesignerId());
        if (designer.role() != UserRole.DESIGNER) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }

        List<String> greetings = List.of(
                "您好，我是" + designer.nickname() + "，接下来由我与您确认需求细节并推进设计方案。",
                DESIGNER_HANDOFF_GREETING
        );
        ConsultantIntakeEntity intake = consultationRepository.saveIntake(ConsultantIntakeEntity.builder()
                .customerId(customer.id())
                .projectType(request.projectType().trim())
                .industry(request.industry().trim())
                .requirementDescription(request.requirementDescription().trim())
                .budgetRange(request.budgetRange().trim())
                .projectCycle(request.projectCycle().trim())
                .status(ConsultantIntakeStatus.MATCHED)
                .matchedDesignerId(designer.id())
                .humanChatId("consultant-" + UUID.randomUUID())
                .greetingMessages(greetings)
                .build());
        greetings.forEach(content -> consultationRepository.saveHumanMessage(
                ConsultantHumanMessageEntity.builder()
                        .humanChatId(intake.getHumanChatId())
                        .senderId(designer.id())
                        .senderRole(MessageSenderRole.DESIGNER)
                        .content(content)
                        .build()));

        return new ConsultantIntakeModels.ConsultantIntakeVO(
                intake.getId(),
                intake.getStatus(),
                new ConsultantIntakeModels.MatchedDesignerVO(
                        designer.id(),
                        designer.nickname(),
                        designer.avatar(),
                        Boolean.TRUE.equals(designerProfile.getOnline()),
                        designerProfile.getSpecialties() == null
                                ? List.of()
                                : List.copyOf(designerProfile.getSpecialties())
                ),
                intake.getHumanChatId(),
                List.copyOf(intake.getGreetingMessages()),
                intake.getCreatedAt().toString()
        );
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

    private ConsultantIntakeEntity accessibleDesignerIntake(Long intakeId, UserProfile designer) {
        ConsultantIntakeEntity intake = consultationRepository.findIntakeById(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!designer.id().equals(intake.getMatchedDesignerId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return intake;
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
        long activeProjectCount = projectRepository
                .countInProgressProjectsByDesigner(intake.getMatchedDesignerId());
        int score = 78
                + (profile != null && Boolean.TRUE.equals(profile.getOnline()) ? 8 : 0)
                + (specialtyMatched ? 6 : 0)
                + (activeProjectCount == 0 ? 4 : 0);
        boolean online = profile != null && Boolean.TRUE.equals(profile.getOnline());
        String reason;
        if (specialtyMatched && online) {
            reason = "当前在线，且您的专业方向与该需求高度匹配";
        } else if (specialtyMatched) {
            reason = "您的专业方向与该需求高度匹配";
        } else if (online && activeProjectCount == 0) {
            reason = "当前在线且项目负载较低，适合及时接待";
        } else {
            reason = "当前项目负载适合接待该客户";
        }
        return new ConsultantIntakeModels.DesignerReceptionVO(
                intake.getId(),
                intake.getStatus(),
                customer.id(),
                customer.nickname(),
                customer.avatar(),
                intake.getProjectType(),
                intake.getIndustry(),
                intake.getRequirementDescription(),
                intake.getBudgetRange(),
                intake.getProjectCycle(),
                Math.min(score, 96),
                reason,
                intake.getHumanChatId(),
                intake.getCreatedAt().toString()
        );
    }

    private DesignerProfileEntity matchDesigner(ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        return consultationRepository.listEnabledDesignerProfiles().stream()
                .filter(profile -> {
                    UserProfile user = identityService.findById(profile.getDesignerId()).orElse(null);
                    return user != null && user.role() == UserRole.DESIGNER
                            && user.status() == UserStatus.ENABLED;
                })
                .min(Comparator
                        .comparing((DesignerProfileEntity profile) -> !Boolean.TRUE.equals(profile.getOnline()))
                        .thenComparingLong(profile -> projectRepository
                                .countInProgressProjectsByDesigner(profile.getDesignerId()))
                        .thenComparing(profile -> !hasSpecialtyMatch(profile, request))
                        .thenComparing(DesignerProfileEntity::getDesignerId))
                .orElseThrow(() -> new ApiException(1001, "当前暂无可匹配的设计师"));
    }

    private boolean hasSpecialtyMatch(
            DesignerProfileEntity profile,
            ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        String projectType = normalize(request.projectType());
        String industry = normalize(request.industry());
        return profile.getSpecialties() != null && profile.getSpecialties().stream()
                .map(this::normalize)
                .anyMatch(specialty -> specialty.equals(projectType) || specialty.equals(industry));
    }

    private UserProfile findUser(Long userId) {
        return identityService.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
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
