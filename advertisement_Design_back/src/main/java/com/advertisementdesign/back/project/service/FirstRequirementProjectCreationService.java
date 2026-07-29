package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;
import com.advertisementdesign.back.common.audit.service.AuditLogWriter;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.idempotency.service.IdempotencyService;
import com.advertisementdesign.back.common.outbox.service.ReliableEventWriter;
import com.advertisementdesign.back.common.storage.service.FirstRequirementAttachmentService;
import com.advertisementdesign.back.communication.service.FirstRequirementConversationService;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.OrganizationMembershipService;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.dto.FirstRequirementRequest;
import com.advertisementdesign.back.project.entity.CustomerProjectMemberEntity;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.CustomerProjectMemberStatus;
import com.advertisementdesign.back.project.enums.CustomerProjectRole;
import com.advertisementdesign.back.project.enums.ProjectNameSource;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.repository.CustomerProjectMemberRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.advertisementdesign.back.project.vo.FirstRequirementResponse;
import com.advertisementdesign.back.workflow.service.ProjectWorkflowInitializationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FirstRequirementProjectCreationService {
    public static final String OPERATION_TYPE = "FIRST_REQUIREMENT_CREATE";

    private final ValidRequirementPolicy validRequirementPolicy;
    private final CurrentActorProvider currentActorProvider;
    private final CurrentUserProfileProvider currentUserProfileProvider;
    private final OrganizationMembershipService organizationMembershipService;
    private final IdempotencyService idempotencyService;
    private final ProjectRepository projectRepository;
    private final CustomerProjectMemberRepository memberRepository;
    private final FirstRequirementAttachmentService attachmentService;
    private final FirstRequirementConversationService conversationService;
    private final ProjectWorkflowInitializationService workflowInitializationService;
    private final AuditLogWriter auditLogWriter;
    private final ReliableEventWriter eventWriter;
    private final ProjectConverter converter;
    private final ObjectMapper objectMapper;

    @Transactional
    public FirstRequirementResponse create(FirstRequirementRequest request, String idempotencyKey) {
        String content = normalizeContent(request.content());
        List<Long> attachmentIds = orderedDistinct(request.fileAssetIds());
        ValidRequirementPolicy.Decision decision = validRequirementPolicy.evaluate(content, !attachmentIds.isEmpty());
        if (!decision.valid()) {
            return converter.invalidRequirement(decision.guidance());
        }

        CurrentActorProvider.CurrentActor current = currentActorProvider.requireCurrentActor();
        ActorRef actor = current.actor();
        IdentityService.UserProfile profile = currentUserProfileProvider.currentUserProfile();
        requireAuthorizedCustomer(actor, profile);

        organizationMembershipService.findActiveOrganization(request.organizationId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "当前组织不可用"));
        OrganizationMembershipService.ActiveOrganizationMember organizationMember =
                organizationMembershipService.findActiveMembership(request.organizationId(), actor.actorId())
                        .orElseThrow(() -> new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "您不是当前组织的有效成员"));

        String requestHash = requestHash(request.organizationId(), content, request.clientMessageId(), attachmentIds);
        IdempotencyService.Claim claim = idempotencyService.claim(
                new IdempotencyService.CommandKey(actor, OPERATION_TYPE, idempotencyKey), requestHash, null);
        if (claim.state() == IdempotencyService.ClaimState.REPLAY_SUCCEEDED) {
            return converter.idempotentReplay(claim.responseSnapshot());
        }
        if (claim.state() == IdempotencyService.ClaimState.IN_PROGRESS) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "相同建项请求正在处理中，请稍后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        String requestId = "first-requirement:" + claim.recordId();
        Map<String, Object> authorizationBasis = Map.of(
                "organizationId", request.organizationId(),
                "organizationMemberId", organizationMember.id(),
                "actorType", actor.type().name());
        String authorizationJson = json(authorizationBasis);

        ProjectEntity project = projectRepository.save(ProjectEntity.builder()
                .organizationId(request.organizationId())
                .name(ProjectEntity.INITIAL_NAME)
                .nameSource(ProjectNameSource.AUTO)
                .description(content)
                .status(ProjectStatus.ACTIVE)
                .startedAt(now)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build());

        memberRepository.save(CustomerProjectMemberEntity.builder()
                .projectId(project.getId())
                .organizationId(request.organizationId())
                .organizationMemberId(organizationMember.id())
                .projectRole(CustomerProjectRole.PRIMARY_CONTACT)
                .canConfirmRequirement(true)
                .canConfirmReport(true)
                .canConfirmDesign(true)
                .canSignContract(true)
                .canManagePayment(true)
                .canReceiveDelivery(true)
                .status(CustomerProjectMemberStatus.ACTIVE)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build());

        List<Long> claimedAttachmentIds = attachmentService.validateAndClaim(
                new FirstRequirementAttachmentService.Command(
                        actor, request.organizationId(), project.getId(), attachmentIds));

        FirstRequirementConversationService.CreatedConversation conversation = conversationService.initialize(
                new FirstRequirementConversationService.Command(
                        project.getId(), actor.actorId(), profile.nickname(), content, authorizationJson,
                        request.clientMessageId().strip(), claimedAttachmentIds, now));

        ProjectWorkflowInitializationService.InitializedWorkflow workflow = workflowInitializationService.initialize(
                new ProjectWorkflowInitializationService.Command(
                        project.getId(), actor, authorizationJson, requestId + ":stage", now));

        auditLogWriter.append(new AuditLogWriter.Entry(
                project.getId(), actor, profile.nickname(), AuditLogEntity.Source.CUSTOMER_UI,
                "PROJECT", project.getId(), String.valueOf(project.getVersion()), "FIRST_REQUIREMENT_PROJECT_CREATED",
                authorizationBasis, Map.of(), Map.of(
                        "projectName", project.getName(),
                        "conversationId", conversation.conversationId(),
                        "currentStage", workflow.stageCode().name()),
                AuditLogEntity.Result.SUCCESS, null, requestId, requestId, now));

        writeOutboxEvents(project, conversation, workflow, requestId, now);

        FirstRequirementResponse response = converter.projectCreated(
                project.getId(), conversation.conversationId(), project.getName(), workflow.stageCode());
        idempotencyService.succeed(claim, "PROJECT", project.getId(), converter.firstRequirementSnapshot(response));
        return response;
    }

    private void writeOutboxEvents(ProjectEntity project,
                                   FirstRequirementConversationService.CreatedConversation conversation,
                                   ProjectWorkflowInitializationService.InitializedWorkflow workflow,
                                   String requestId, LocalDateTime occurredAt) {
        Map<String, Object> commonPayload = Map.of(
                "projectId", project.getId(),
                "conversationId", conversation.conversationId(),
                "stage", workflow.stageCode().name(),
                "occurredAt", occurredAt.toString());
        eventWriter.write(new ReliableEventWriter.Event(
                "PROJECT", project.getId(), "PROJECT_AUTO_NAMING_REQUESTED",
                requestId + ":auto-naming", commonPayload));
        eventWriter.write(new ReliableEventWriter.Event(
                "PROJECT", project.getId(), "RESPONSIBLE_DESIGNER_ASSIGNMENT_REQUESTED",
                requestId + ":designer-assignment", commonPayload));
        eventWriter.write(new ReliableEventWriter.Event(
                "PROJECT", project.getId(), "FIRST_REQUIREMENT_GUIDANCE_REQUESTED",
                requestId + ":requirement-guidance", commonPayload));
    }

    private void requireAuthorizedCustomer(ActorRef actor, IdentityService.UserProfile profile) {
        if (actor.type() != ActorRef.ActorType.CUSTOMER_USER
                || profile.role() != UserRole.CUSTOMER
                || profile.status() != UserStatus.ENABLED
                || !actor.actorId().equals(profile.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "仅有效客户账号可以创建项目");
        }
    }

    private List<Long> orderedDistinct(List<Long> ids) {
        return ids == null ? List.of() : List.copyOf(new LinkedHashSet<>(ids));
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.strip();
    }

    private String requestHash(Long organizationId, String content, String clientMessageId, List<Long> attachmentIds) {
        Map<String, Object> canonicalCommand = new LinkedHashMap<>();
        canonicalCommand.put("organizationId", organizationId);
        canonicalCommand.put("content", content);
        canonicalCommand.put("clientMessageId", clientMessageId.strip());
        canonicalCommand.put("attachmentIds", attachmentIds);
        String canonical = json(canonicalCommand);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize first requirement command", exception);
        }
    }
}
