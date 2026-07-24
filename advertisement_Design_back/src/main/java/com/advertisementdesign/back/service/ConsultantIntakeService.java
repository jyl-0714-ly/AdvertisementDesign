package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.consultant.ConsultantIntakeModels;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.domain.entity.ConsultantHumanMessageEntity;
import com.advertisementdesign.back.domain.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.domain.entity.DesignerProfileEntity;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.domain.enums.UserStatus;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultantIntakeService {
    private static final String DESIGNER_HANDOFF_GREETING = "您的需求已整理完成，请稍等。";

    private final DemoDataStore store;
    private final AuthService authService;

    public ConsultantIntakeModels.ConsultantIntakeVO submit(
            ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        UserEntity customer = authService.currentUserEntity();
        ensureCustomer(customer);

        DesignerProfileEntity designerProfile = matchDesigner(request);
        UserEntity designer = store.findUserById(designerProfile.getDesignerId())
                .filter(user -> user.getRole() == UserRole.DESIGNER)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));

        List<String> greetings = List.of(
                "您好，我是" + designer.getNickname() + "，接下来由我与您确认需求细节并推进设计方案。",
                DESIGNER_HANDOFF_GREETING
        );
        ConsultantIntakeEntity intake = store.saveConsultantIntake(ConsultantIntakeEntity.builder()
                .customerId(customer.getId())
                .projectType(request.projectType().trim())
                .industry(request.industry().trim())
                .requirementDescription(request.requirementDescription().trim())
                .budgetRange(request.budgetRange().trim())
                .projectCycle(request.projectCycle().trim())
                .status(ConsultantIntakeStatus.MATCHED)
                .matchedDesignerId(designer.getId())
                .humanChatId("consultant-" + UUID.randomUUID())
                .greetingMessages(greetings)
                .build());
        greetings.forEach(content -> store.saveConsultantHumanMessage(ConsultantHumanMessageEntity.builder()
                .humanChatId(intake.getHumanChatId())
                .senderId(designer.getId())
                .senderRole(MessageSenderRole.DESIGNER)
                .content(content)
                .build()));

        return new ConsultantIntakeModels.ConsultantIntakeVO(
                intake.getId(),
                intake.getStatus(),
                new ConsultantIntakeModels.MatchedDesignerVO(
                        designer.getId(),
                        designer.getNickname(),
                        designer.getAvatar(),
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
        UserEntity designer = authService.currentUserEntity();
        ensureDesigner(designer);
        return store.listConsultantIntakesByDesigner(designer.getId()).stream()
                .map(this::toDesignerReceptionVO)
                .toList();
    }

    public ConsultantIntakeModels.DesignerReceptionVO getDesignerReception(Long intakeId) {
        UserEntity designer = authService.currentUserEntity();
        ensureDesigner(designer);
        return toDesignerReceptionVO(accessibleDesignerIntake(intakeId, designer));
    }

    public ConsultantIntakeModels.DesignerReceptionVO accept(Long intakeId) {
        UserEntity designer = authService.currentUserEntity();
        ensureDesigner(designer);
        ConsultantIntakeEntity intake = accessibleDesignerIntake(intakeId, designer);
        if (intake.getStatus() == ConsultantIntakeStatus.MATCHED) {
            intake.setStatus(ConsultantIntakeStatus.ACCEPTED);
            store.saveConsultantIntake(intake);
        }
        return toDesignerReceptionVO(intake);
    }

    private ConsultantIntakeEntity accessibleDesignerIntake(Long intakeId, UserEntity designer) {
        ConsultantIntakeEntity intake = store.findConsultantIntakeById(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!designer.getId().equals(intake.getMatchedDesignerId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return intake;
    }

    private ConsultantIntakeModels.DesignerReceptionVO toDesignerReceptionVO(ConsultantIntakeEntity intake) {
        UserEntity customer = store.findUserById(intake.getCustomerId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        DesignerProfileEntity profile = store.findDesignerProfile(intake.getMatchedDesignerId()).orElse(null);
        boolean specialtyMatched = profile != null && profile.getSpecialties() != null
                && profile.getSpecialties().stream()
                .map(this::normalize)
                .anyMatch(specialty -> specialty.equals(normalize(intake.getProjectType()))
                        || specialty.equals(normalize(intake.getIndustry())));
        int score = 78
                + (profile != null && Boolean.TRUE.equals(profile.getOnline()) ? 8 : 0)
                + (specialtyMatched ? 6 : 0)
                + (store.countInProgressProjectsByDesigner(intake.getMatchedDesignerId()) == 0 ? 4 : 0);
        boolean online = profile != null && Boolean.TRUE.equals(profile.getOnline());
        long activeProjectCount = store.countInProgressProjectsByDesigner(intake.getMatchedDesignerId());
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
                customer.getId(),
                customer.getNickname(),
                customer.getAvatar(),
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
        return store.listDesignerProfiles().stream()
                .filter(profile -> Boolean.TRUE.equals(profile.getEnabled()))
                .filter(profile -> store.findUserById(profile.getDesignerId())
                        .map(user -> user.getRole() == UserRole.DESIGNER && user.getStatus() == UserStatus.ENABLED)
                        .orElse(false))
                .min(Comparator
                        .comparing((DesignerProfileEntity profile) -> !Boolean.TRUE.equals(profile.getOnline()))
                        .thenComparingLong(profile -> store.countInProgressProjectsByDesigner(profile.getDesignerId()))
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureDesigner(UserEntity user) {
        if (user.getRole() != UserRole.DESIGNER || user.getStatus() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private void ensureCustomer(UserEntity user) {
        if (user.getRole() != UserRole.CUSTOMER || user.getStatus() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
