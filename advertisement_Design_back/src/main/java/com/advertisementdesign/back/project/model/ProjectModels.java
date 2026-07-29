package com.advertisementdesign.back.project.model;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.project.enums.CustomerProjectMemberStatus;
import com.advertisementdesign.back.project.enums.CustomerProjectRole;
import com.advertisementdesign.back.project.enums.ProjectAssignmentRole;
import com.advertisementdesign.back.project.enums.ProjectAssignmentStatus;
import com.advertisementdesign.back.project.enums.ProjectNameSource;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "项目公开应用契约")
public final class ProjectModels {
    private ProjectModels() {
    }

    @Schema(description = "项目存在性及版本上下文")
    public record ProjectContextView(Long projectId, Long organizationId, ProjectStatus status, Long version) {
    }

    @Schema(description = "隐私收敛项目摘要；结构上不包含需求正文、组织、客户、内部主体或商业信息")
    public record ProjectSummaryView(
            Long id,
            String name,
            ProjectStatus status,
            LocalDateTime startedAt,
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "经完整项目权限校验后的项目详情")
    public record ProjectFullDetailView(
            Long id,
            Long organizationId,
            String name,
            ProjectNameSource nameSource,
            String description,
            ProjectStatus status,
            Long confirmedRequirementVersionId,
            LocalDateTime startedAt,
            LocalDateTime pausedAt,
            LocalDateTime completedAt,
            LocalDateTime terminatedAt,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CustomerMemberView(
            Long id,
            Long projectId,
            Long organizationId,
            Long organizationMemberId,
            CustomerProjectRole projectRole,
            boolean canConfirmRequirement,
            boolean canConfirmReport,
            boolean canConfirmDesign,
            boolean canSignContract,
            boolean canManagePayment,
            boolean canReceiveDelivery,
            CustomerProjectMemberStatus status,
            Long version
    ) {
    }

    public record AssignmentView(
            Long id,
            Long projectId,
            Long designerUserId,
            ProjectAssignmentRole assignmentRole,
            Set<String> authorizationScopes,
            ProjectAssignmentStatus status,
            ActorRef initiatedBy,
            LocalDateTime acceptedAt,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public AssignmentView {
            authorizationScopes = authorizationScopes == null ? Set.of() : Set.copyOf(authorizationScopes);
        }
    }
}
