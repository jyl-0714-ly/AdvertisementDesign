package com.advertisementdesign.back.domain.entity;

import com.advertisementdesign.back.domain.enums.ConsultantIntakeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantIntakeEntity {
    private Long id;
    private Long customerId;
    private String projectType;
    private String industry;
    private String requirementDescription;
    private String budgetRange;
    private String projectCycle;
    private ConsultantIntakeStatus status;
    private Long matchedDesignerId;
    private String humanChatId;
    private List<String> greetingMessages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
