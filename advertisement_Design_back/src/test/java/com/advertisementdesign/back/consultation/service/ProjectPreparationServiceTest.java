package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectPreparationServiceTest {
    @Mock private ConsultationRepository consultationRepository;
    @Mock private AuthService authService;

    private ProjectPreparationService service;

    @BeforeEach
    void setUp() {
        service = new ProjectPreparationService(consultationRepository, authService);
        when(authService.currentUserProfile()).thenReturn(
                new UserProfile(2L, "设计师", UserRole.DESIGNER, null, UserStatus.ENABLED));
    }

    @Test
    void rejectsWrongDesigner() {
        ConsultantIntakeEntity intake = intake(ConsultantIntakeStatus.ACCEPTED);
        intake.setMatchedDesignerId(3L);
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.confirmContract(7L)).getCode());
    }

    @Test
    void rejectsIntakeThatIsNotAccepted() {
        when(consultationRepository.findIntakeByIdForUpdate(7L))
                .thenReturn(Optional.of(intake(ConsultantIntakeStatus.MATCHED)));
        assertEquals(400, assertThrows(ApiException.class,
                () -> service.confirmInitialPayment(7L)).getCode());
    }

    @Test
    void confirmationIsIdempotent() {
        ConsultantIntakeEntity intake = intake(ConsultantIntakeStatus.ACCEPTED);
        intake.setContractConfirmedAt(LocalDateTime.now().minusDays(1));
        when(consultationRepository.findIntakeByIdForUpdate(7L)).thenReturn(Optional.of(intake));
        assertTrue(service.confirmContract(7L).contractConfirmed());
    }

    private ConsultantIntakeEntity intake(ConsultantIntakeStatus status) {
        return ConsultantIntakeEntity.builder().id(7L).customerId(1L).matchedDesignerId(2L)
                .projectType("品牌设计").requirementDescription("需求").status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
