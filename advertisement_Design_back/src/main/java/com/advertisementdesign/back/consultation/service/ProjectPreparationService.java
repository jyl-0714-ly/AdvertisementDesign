package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.model.ProjectPreparationModels;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectPreparationService {
    private final ConsultationRepository consultationRepository;
    private final AuthService authService;

    public ProjectPreparationModels.ProjectPreparation get(Long intakeId) {
        UserProfile designer = currentEnabledDesigner();
        ConsultantIntakeEntity intake = consultationRepository.findIntakeById(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        validateAcceptedMatch(intake, designer);
        return toPreparation(intake);
    }

    @Transactional
    public ProjectPreparationModels.ProjectPreparation confirmContract(Long intakeId) {
        ConsultantIntakeEntity intake = lockedAcceptedIntake(intakeId);
        if (intake.getContractConfirmedAt() == null) {
            intake.setContractConfirmedAt(LocalDateTime.now());
            consultationRepository.saveIntake(intake);
        }
        return toPreparation(intake);
    }

    @Transactional
    public ProjectPreparationModels.ProjectPreparation confirmInitialPayment(Long intakeId) {
        ConsultantIntakeEntity intake = lockedAcceptedIntake(intakeId);
        if (intake.getInitialPaymentConfirmedAt() == null) {
            intake.setInitialPaymentConfirmedAt(LocalDateTime.now());
            consultationRepository.saveIntake(intake);
        }
        return toPreparation(intake);
    }

    @Transactional
    public ProjectPreparationModels.ProjectPreparation lockForProjectCreation(Long intakeId) {
        return toPreparation(lockedAcceptedIntake(intakeId));
    }

    private ConsultantIntakeEntity lockedAcceptedIntake(Long intakeId) {
        UserProfile designer = currentEnabledDesigner();
        ConsultantIntakeEntity intake = consultationRepository.findIntakeByIdForUpdate(intakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        validateAcceptedMatch(intake, designer);
        return intake;
    }

    private UserProfile currentEnabledDesigner() {
        UserProfile user = authService.currentUserProfile();
        if (user.role() != UserRole.DESIGNER || user.status() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return user;
    }

    private void validateAcceptedMatch(ConsultantIntakeEntity intake, UserProfile designer) {
        if (!Objects.equals(intake.getMatchedDesignerId(), designer.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (intake.getStatus() != ConsultantIntakeStatus.ACCEPTED) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "仅已接待的咨询可进入项目准备");
        }
    }

    private ProjectPreparationModels.ProjectPreparation toPreparation(ConsultantIntakeEntity intake) {
        return new ProjectPreparationModels.ProjectPreparation(
                intake.getId(), intake.getCustomerId(), intake.getMatchedDesignerId(),
                intake.getProjectType(), intake.getRequirementDescription(),
                intake.getContractConfirmedAt() != null, intake.getContractConfirmedAt(),
                intake.getInitialPaymentConfirmedAt() != null, intake.getInitialPaymentConfirmedAt());
    }
}
