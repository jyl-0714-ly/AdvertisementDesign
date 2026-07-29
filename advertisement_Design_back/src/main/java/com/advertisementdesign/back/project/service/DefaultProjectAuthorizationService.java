package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.OrganizationMembershipService;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.entity.CustomerProjectMemberEntity;
import com.advertisementdesign.back.project.entity.ProjectAssignmentEntity;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.CustomerProjectRole;
import com.advertisementdesign.back.project.enums.ProjectAssignmentRole;
import com.advertisementdesign.back.project.repository.CustomerProjectMemberRepository;
import com.advertisementdesign.back.project.repository.ProjectAssignmentRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultProjectAuthorizationService implements ProjectAuthorizationService {
    private final CurrentActorProvider currentActorProvider;
    private final OrganizationMembershipService organizationMembershipService;
    private final ProjectRepository projectRepository;
    private final CustomerProjectMemberRepository customerMemberRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final ProjectConverter converter;

    @Override
    public ProjectFileAccessDecision authorizeProjectFile(Long fileId) {
        // Project no longer owns the obsolete project_file table. Artifact/storage migration will
        // replace this compatibility boundary; until then it must not claim an association.
        return new ProjectFileAccessDecision(false, false);
    }

    @Override
    public AuthorizationDecision authorize(Long projectId, ProjectAction action) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        LocalDateTime decidedAt = LocalDateTime.now();
        return switch (actor.type()) {
            case ADMIN_USER -> allow(projectId, action, actor, decidedAt, AccessLevel.ADMIN,
                    "ADMIN_ACCOUNT", actor.actorId(), null, null, null, Set.of("ALL"));
            case CUSTOMER_USER -> authorizeCustomer(project, actor, action, decidedAt);
            case DESIGNER_USER -> authorizeDesigner(project, actor, action, decidedAt);
            default -> deny(projectId, action, actor, decidedAt, "UNSUPPORTED_ACTOR", null, null, null, null);
        };
    }

    private AuthorizationDecision authorizeCustomer(ProjectEntity project, ActorRef actor,
                                                     ProjectAction action, LocalDateTime decidedAt) {
        if (organizationMembershipService.findActiveOrganization(project.getOrganizationId()).isEmpty()) {
            return deny(project.getId(), action, actor, decidedAt,
                    "ORGANIZATION_NOT_ACTIVE", null, null, null, null);
        }
        var organizationMember = organizationMembershipService
                .findActiveMembership(project.getOrganizationId(), actor.actorId()).orElse(null);
        if (organizationMember == null) {
            return deny(project.getId(), action, actor, decidedAt,
                    "NO_ACTIVE_ORGANIZATION_MEMBERSHIP", null, null, null, null);
        }
        CustomerProjectMemberEntity member = customerMemberRepository
                .findActiveByProjectAndOrganizationMember(
                        project.getId(), project.getOrganizationId(), organizationMember.id())
                .orElse(null);
        if (member == null) {
            return deny(project.getId(), action, actor, decidedAt, "NO_ACTIVE_PROJECT_MEMBERSHIP",
                    null, null, organizationMember.id(), organizationMember.version());
        }
        Set<String> scopes = customerScopes(member);
        boolean allowed = switch (action) {
            case VIEW_SUMMARY, VIEW_FULL -> true;
            case SEND_MESSAGE -> member.getProjectRole() == CustomerProjectRole.PRIMARY_CONTACT
                    || member.getProjectRole() == CustomerProjectRole.CONFIRMATION_MEMBER
                    || member.getProjectRole() == CustomerProjectRole.COMMENT_ONLY;
            case VIEW_SENSITIVE, VIEW_COMMERCIAL, UPDATE_PROJECT, MANAGE_PROJECT_FILE,
                    TAKEOVER, REVIEW_ARTIFACT, ADJUST_ASSIGNMENT -> false;
            case CONFIRM_REQUIREMENT -> Boolean.TRUE.equals(member.getCanConfirmRequirement());
            case CONFIRM_REPORT -> Boolean.TRUE.equals(member.getCanConfirmReport());
            case CONFIRM_DESIGN -> Boolean.TRUE.equals(member.getCanConfirmDesign());
            case SIGN_CONTRACT -> Boolean.TRUE.equals(member.getCanSignContract());
            case MANAGE_PAYMENT -> Boolean.TRUE.equals(member.getCanManagePayment());
            case RECEIVE_DELIVERY -> Boolean.TRUE.equals(member.getCanReceiveDelivery());
        };
        return allowed
                ? allow(project.getId(), action, actor, decidedAt, AccessLevel.FULL, "CUSTOMER_PROJECT_MEMBER",
                        member.getId(), member.getVersion(), organizationMember.id(), organizationMember.version(), scopes)
                : deny(project.getId(), action, actor, decidedAt, "CUSTOMER_ACTION_NOT_GRANTED",
                        member.getId(), member.getVersion(), organizationMember.id(), organizationMember.version());
    }

    private AuthorizationDecision authorizeDesigner(ProjectEntity project, ActorRef actor,
                                                     ProjectAction action, LocalDateTime decidedAt) {
        ProjectAssignmentEntity assignment = assignmentRepository
                .findEffectiveAssignment(project.getId(), actor.actorId(), decidedAt).orElse(null);
        if (assignment == null) {
            return action == ProjectAction.VIEW_SUMMARY
                    ? allow(project.getId(), action, actor, decidedAt, AccessLevel.SUMMARY,
                    "ENABLED_DESIGNER", actor.actorId(), null, null, null, Set.of("PRIVACY_REDUCED"))
                    : deny(project.getId(), action, actor, decidedAt,
                    "NO_EFFECTIVE_ASSIGNMENT", null, null, null, null);
        }
        Set<String> scopes = converter.parseScopes(assignment.getAuthorizationScope());
        boolean primary = assignment.getAssignmentRole() == ProjectAssignmentRole.PRIMARY_DESIGNER;
        boolean allowed = switch (action) {
            case VIEW_SUMMARY, VIEW_FULL, SEND_MESSAGE -> primary || scopes.contains("FULL");
            case VIEW_SENSITIVE -> primary || scopes.contains("SENSITIVE");
            case VIEW_COMMERCIAL -> scopes.contains("COMMERCIAL");
            case UPDATE_PROJECT -> primary || scopes.contains("UPDATE_PROJECT");
            case MANAGE_PROJECT_FILE -> primary || scopes.contains("MANAGE_PROJECT_FILE");
            case CONFIRM_REQUIREMENT, CONFIRM_REPORT, CONFIRM_DESIGN, SIGN_CONTRACT,
                    MANAGE_PAYMENT, RECEIVE_DELIVERY -> false;
            case TAKEOVER -> primary || scopes.contains("TAKEOVER");
            case REVIEW_ARTIFACT -> primary || assignment.getAssignmentRole() == ProjectAssignmentRole.REVIEWER
                    || scopes.contains("REVIEW_ARTIFACT");
            case ADJUST_ASSIGNMENT -> scopes.contains("ADJUST_ASSIGNMENT");
        };
        AccessLevel level = scopes.contains("COMMERCIAL") ? AccessLevel.COMMERCIAL
                : scopes.contains("SENSITIVE") || primary ? AccessLevel.SENSITIVE : AccessLevel.FULL;
        return allowed
                ? allow(project.getId(), action, actor, decidedAt, level, "PROJECT_ASSIGNMENT",
                assignment.getId(), assignment.getVersion(), null, null, scopes)
                : deny(project.getId(), action, actor, decidedAt, "ASSIGNMENT_SCOPE_INSUFFICIENT",
                assignment.getId(), assignment.getVersion(), null, null);
    }

    private Set<String> customerScopes(CustomerProjectMemberEntity member) {
        Set<String> scopes = new HashSet<>();
        if (member.getProjectRole() != null) scopes.add(member.getProjectRole().name());
        if (Boolean.TRUE.equals(member.getCanConfirmRequirement())) scopes.add("CONFIRM_REQUIREMENT");
        if (Boolean.TRUE.equals(member.getCanConfirmReport())) scopes.add("CONFIRM_REPORT");
        if (Boolean.TRUE.equals(member.getCanConfirmDesign())) scopes.add("CONFIRM_DESIGN");
        if (Boolean.TRUE.equals(member.getCanSignContract())) scopes.add("SIGN_CONTRACT");
        if (Boolean.TRUE.equals(member.getCanManagePayment())) scopes.add("MANAGE_PAYMENT");
        if (Boolean.TRUE.equals(member.getCanReceiveDelivery())) scopes.add("RECEIVE_DELIVERY");
        return Set.copyOf(scopes);
    }

    private AuthorizationDecision allow(Long projectId, ProjectAction action, ActorRef actor, LocalDateTime decidedAt,
                                        AccessLevel level, String source, Long relationshipId, Long relationshipVersion,
                                        Long organizationMembershipId, Long organizationMembershipVersion,
                                        Set<String> scopes) {
        return new AuthorizationDecision(true, level, new AuthorizationBasis(source, projectId, action, actor, decidedAt,
                relationshipId, relationshipVersion, organizationMembershipId, organizationMembershipVersion, scopes));
    }

    private AuthorizationDecision deny(Long projectId, ProjectAction action, ActorRef actor, LocalDateTime decidedAt,
                                       String source, Long relationshipId, Long relationshipVersion,
                                       Long organizationMembershipId, Long organizationMembershipVersion) {
        return new AuthorizationDecision(false, AccessLevel.NONE,
                new AuthorizationBasis(source, projectId, action, actor, decidedAt, relationshipId,
                        relationshipVersion, organizationMembershipId, organizationMembershipVersion, Set.of()));
    }
}
