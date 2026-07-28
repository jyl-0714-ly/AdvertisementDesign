package com.advertisementdesign.back.consultation.model;

import java.time.LocalDateTime;

public final class ProjectPreparationModels {
    private ProjectPreparationModels() {
    }

    public record ProjectPreparation(
            Long intakeId,
            Long customerId,
            Long designerId,
            String projectType,
            String requirementDescription,
            boolean contractConfirmed,
            LocalDateTime contractConfirmedAt,
            boolean initialPaymentConfirmed,
            LocalDateTime initialPaymentConfirmedAt) {
    }
}
