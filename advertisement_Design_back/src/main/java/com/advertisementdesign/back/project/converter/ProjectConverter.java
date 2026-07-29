package com.advertisementdesign.back.project.converter;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.project.entity.CustomerProjectMemberEntity;
import com.advertisementdesign.back.project.entity.ProjectAssignmentEntity;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProjectConverter {
    private final ObjectMapper objectMapper;

    public ProjectModels.ProjectContextView toContext(ProjectEntity entity) {
        return new ProjectModels.ProjectContextView(
                entity.getId(), entity.getOrganizationId(), entity.getStatus(), entity.getVersion());
    }

    public ProjectModels.ProjectSummaryView toSummary(ProjectEntity entity) {
        return new ProjectModels.ProjectSummaryView(
                entity.getId(), entity.getName(), entity.getStatus(), entity.getStartedAt(), entity.getUpdatedAt());
    }

    public ProjectModels.ProjectFullDetailView toFullDetail(ProjectEntity entity) {
        return new ProjectModels.ProjectFullDetailView(
                entity.getId(), entity.getOrganizationId(), entity.getName(), entity.getNameSource(),
                entity.getDescription(), entity.getStatus(), entity.getConfirmedRequirementVersionId(),
                entity.getStartedAt(), entity.getPausedAt(), entity.getCompletedAt(), entity.getTerminatedAt(),
                entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public ProjectModels.CustomerMemberView toCustomerMember(CustomerProjectMemberEntity entity) {
        return new ProjectModels.CustomerMemberView(
                entity.getId(), entity.getProjectId(), entity.getOrganizationId(), entity.getOrganizationMemberId(),
                entity.getProjectRole(), Boolean.TRUE.equals(entity.getCanConfirmRequirement()),
                Boolean.TRUE.equals(entity.getCanConfirmReport()), Boolean.TRUE.equals(entity.getCanConfirmDesign()),
                Boolean.TRUE.equals(entity.getCanSignContract()), Boolean.TRUE.equals(entity.getCanManagePayment()),
                Boolean.TRUE.equals(entity.getCanReceiveDelivery()), entity.getStatus(), entity.getVersion());
    }

    public ProjectModels.AssignmentView toAssignment(ProjectAssignmentEntity entity) {
        ActorRef initiatedBy = new ActorRef(entity.getInitiatedByActorType(), entity.getInitiatedByActorId());
        return new ProjectModels.AssignmentView(
                entity.getId(), entity.getProjectId(), entity.getDesignerUserId(), entity.getAssignmentRole(),
                parseScopes(entity.getAuthorizationScope()), entity.getStatus(), initiatedBy, entity.getAcceptedAt(),
                entity.getEffectiveFrom(), entity.getEffectiveTo(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public Set<String> parseScopes(String json) {
        if (json == null || json.isBlank()) return Set.of();
        try {
            Object value = objectMapper.readValue(json, Object.class);
            Set<String> scopes = new HashSet<>();
            if (value instanceof Collection<?> collection) {
                collection.forEach(item -> scopes.add(String.valueOf(item)));
            } else if (value instanceof Map<?, ?> map) {
                map.forEach((key, enabled) -> {
                    if (Boolean.TRUE.equals(enabled)) scopes.add(String.valueOf(key));
                });
            }
            return Set.copyOf(scopes);
        } catch (Exception exception) {
            return Set.of();
        }
    }
}
