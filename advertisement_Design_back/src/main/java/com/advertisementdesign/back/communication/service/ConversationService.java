package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;
import com.advertisementdesign.back.common.audit.service.AuditLogWriter;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.communication.converter.ConversationConverter;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.MessageSendSource;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final CommunicationRepository repository;
    private final ConversationConverter converter;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectQueryService projectQueryService;
    private final CurrentActorProvider currentActorProvider;
    private final CurrentUserProfileProvider currentUserProfileProvider;
    private final FileService fileService;
    private final AuditLogWriter auditLogWriter;
    private final ObjectMapper objectMapper;

    public ConversationModels.ConversationView conversation(Long projectId) {
        requireProjectAccess(projectId);
        return converter.toConversation(requireConversation(projectId));
    }

    public ConversationModels.MessagePage customerMessages(
            Long projectId, Long beforeMessageId, long size) {
        requireProjectAccess(projectId);
        ConversationEntity conversation = requireConversation(projectId);
        int pageSize = (int) Math.max(1, Math.min(size, 100));
        List<MessageEntity> rows = repository.listMessages(conversation.getId(), beforeMessageId, pageSize + 1L);
        boolean hasMore = rows.size() > pageSize;
        List<MessageEntity> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<ConversationModels.CustomerMessageView> items = pageRows.stream()
                .map(this::toCustomerMessage)
                .toList();
        Long nextCursor = hasMore && !pageRows.isEmpty() ? pageRows.get(pageRows.size() - 1).getId() : null;
        return new ConversationModels.MessagePage(items, nextCursor, hasMore);
    }

    /**
     * Appends on behalf of the authenticated user. Actor, source, display identity and authorization
     * evidence are derived here and cannot be asserted by the caller.
     */
    @Transactional
    public ConversationModels.CustomerMessageView appendAsCurrentUser(
            ConversationModels.CurrentUserAppendCommand command) {
        CurrentActorProvider.CurrentActor currentActor = currentActorProvider.requireCurrentActor();
        ProjectAuthorizationService.AuthorizationDecision decision = authorizationService.authorize(
                command.projectId(), ProjectAuthorizationService.ProjectAction.SEND_MESSAGE);
        if (!decision.allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        String displayIdentity = currentActor.actor().type() == ActorRef.ActorType.CUSTOMER_USER
                ? requireCustomerIdentity(currentUserProfileProvider.currentUserProfile().nickname())
                : ConversationModels.SERVICE_TEAM_IDENTITY;
        ProjectModels.ProjectContextView context = projectQueryService.findContext(command.projectId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ConversationEntity conversation = requireConversation(context.projectId());
        String clientMessageId = requireClientMessageId(command.clientMessageId());
        var existing = repository.findMessageByClientMessageId(conversation.getId(), clientMessageId);
        if (existing.isPresent()) {
            return toCustomerMessage(existing.get());
        }
        fileService.claimProjectMessageDrafts(
                context.projectId(), context.organizationId(), currentActor.actor(), command.fileAssetIds());
        ConversationModels.InternalMessageView appended = appendValidated(
                new ConversationModels.TrustedInternalAppendCommand(
                        command.projectId(), customerMessageType(command.content(), command.fileAssetIds()),
                        normalizedContent(command.content()), displayIdentity,
                        currentActor.actor(), sourceFor(currentActor.actor()), decision.basis(), command.replyToMessageId(),
                        command.correctionMessageId(), clientMessageId, command.fileAssetIds(), null));
        auditMessage(command.projectId(), appended, currentActor.actor(), displayIdentity, decision.basis());
        return toCustomerMessage(repository.findMessage(conversation.getId(), appended.id())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND)));
    }

    /**
     * Trusted internal append-only boundary for future atomic project/conversation orchestration.
     * It accepts pre-established actor and authorization evidence and is deliberately not a controller contract.
     * Attachments require a separate validated attachment command and are rejected by this foundation boundary.
     */
    @Transactional
    public ConversationModels.InternalMessageView appendTrustedInternal(
            ConversationModels.TrustedInternalAppendCommand command) {
        if (!command.fileAssetIds().isEmpty()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(),
                    "附件必须通过已校验草稿所有权、文件状态和项目关系的附件命令发送");
        }
        return appendValidated(command);
    }

    private ConversationModels.InternalMessageView appendValidated(
            ConversationModels.TrustedInternalAppendCommand command) {
        ProjectModels.ProjectContextView context = projectQueryService.findContext(command.projectId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ConversationEntity conversation = requireConversation(context.projectId());
        validateTrustedCommand(conversation, command);
        MessageEntity message = MessageEntity.builder()
                .conversationId(conversation.getId()).messageType(command.messageType()).content(command.content())
                .customerDisplayIdentity(customerDisplayIdentity(command)).actorType(command.actor().type())
                .actorId(command.actor().actorId()).sendSource(command.sendSource())
                .authorizationBasis(serializeAuthorizationBasis(command.authorizationBasis()))
                .replyToMessageId(command.replyToMessageId()).correctionMessageId(command.correctionMessageId())
                .clientMessageId(command.clientMessageId())
                .sentAt(command.sentAt() == null ? LocalDateTime.now() : command.sentAt()).build();
        repository.appendMessage(message, command.fileAssetIds());
        repository.updateLastMessage(conversation, message);
        return converter.toInternalMessage(message, repository.listAttachments(message.getId()));
    }

    private void validateTrustedCommand(ConversationEntity conversation,
                                        ConversationModels.TrustedInternalAppendCommand command) {
        if (command.actor() == null || command.messageType() == null || command.sendSource() == null
                || command.authorizationBasis() == null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        validateAuthorizationEvidence(command);
        validateActorSource(command.actor(), command.sendSource());
        if (command.actor().type() == ActorRef.ActorType.CUSTOMER_USER) {
            requireCustomerIdentity(command.customerDisplayIdentity());
        }
        if ((command.content() == null || command.content().isBlank()) && command.fileAssetIds().isEmpty()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "消息正文和附件不能同时为空");
        }
        validateTarget(conversation.getId(), command.replyToMessageId());
        validateCorrection(conversation.getId(), command);
    }

    private void validateAuthorizationEvidence(ConversationModels.TrustedInternalAppendCommand command) {
        ProjectAuthorizationService.AuthorizationBasis basis = command.authorizationBasis();
        if (!Objects.equals(command.projectId(), basis.projectId())
                || basis.action() != ProjectAuthorizationService.ProjectAction.SEND_MESSAGE
                || !Objects.equals(command.actor(), basis.actor())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "消息授权依据与发送命令不匹配");
        }
    }

    private void validateActorSource(ActorRef actor, MessageSendSource sendSource) {
        MessageSendSource expectedSource = switch (actor.type()) {
            case CUSTOMER_USER -> MessageSendSource.CUSTOMER_UI;
            case DESIGNER_USER -> MessageSendSource.DESIGNER_UI;
            case ADMIN_USER -> MessageSendSource.ADMIN_UI;
            case COORDINATOR_AGENT, STAGE_AGENT -> MessageSendSource.AUTOMATION;
            case SYSTEM_EVENT -> MessageSendSource.SYSTEM;
        };
        if (sendSource != expectedSource) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "消息发送主体与来源不匹配");
        }
    }

    private void validateCorrection(Long conversationId,
                                    ConversationModels.TrustedInternalAppendCommand command) {
        if (command.correctionMessageId() == null) {
            return;
        }
        MessageEntity original = repository.findMessage(conversationId, command.correctionMessageId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.BAD_REQUEST.getCode(),
                        "更正的原消息不属于当前项目会话"));
        if (original.getCorrectionMessageId() != null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "更正消息不能再次作为原消息更正");
        }
        if (original.getActorType() != command.actor().type()
                || !Objects.equals(original.getActorId(), command.actor().actorId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "只能更正自己发送的消息");
        }
        if (repository.findCorrectionForOriginal(conversationId, original.getId()).isPresent()) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "这条消息已经有更正内容");
        }
    }

    private void validateTarget(Long conversationId, Long targetMessageId) {
        if (targetMessageId != null && repository.findMessage(conversationId, targetMessageId).isEmpty()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "消息关联目标不属于当前项目会话");
        }
    }

    private String customerDisplayIdentity(ConversationModels.TrustedInternalAppendCommand command) {
        return command.actor().type() == ActorRef.ActorType.CUSTOMER_USER
                ? requireCustomerIdentity(command.customerDisplayIdentity())
                : ConversationModels.SERVICE_TEAM_IDENTITY;
    }

    private MessageSendSource sourceFor(ActorRef actor) {
        return switch (actor.type()) {
            case CUSTOMER_USER -> MessageSendSource.CUSTOMER_UI;
            case DESIGNER_USER -> MessageSendSource.DESIGNER_UI;
            case ADMIN_USER -> MessageSendSource.ADMIN_UI;
            default -> throw new ApiException(ApiErrorCode.FORBIDDEN);
        };
    }

    private String serializeAuthorizationBasis(ProjectAuthorizationService.AuthorizationBasis basis) {
        try {
            return objectMapper.writeValueAsString(basis);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize authorization basis", exception);
        }
    }

    private ConversationModels.CustomerMessageView toCustomerMessage(MessageEntity message) {
        var attachments = repository.listAttachments(message.getId()).stream().map(attachment -> {
            FileModels.CustomerSafeFileMetadata file = fileService.customerSafeMetadata(attachment.getFileAssetId());
            return new ConversationModels.AttachmentView(
                    attachment.getId(), attachment.getFileAssetId(), attachment.getDisplayOrder(),
                    file.name(), file.mimeType(), file.size(), file.downloadPath(), attachment.getCreatedAt());
        }).toList();
        return converter.toCustomerMessageView(message, attachments);
    }

    private MessageType customerMessageType(String content, List<Long> fileAssetIds) {
        boolean hasContent = content != null && !content.isBlank();
        boolean hasFiles = fileAssetIds != null && !fileAssetIds.isEmpty();
        if (hasContent && hasFiles) return MessageType.MIXED;
        if (hasFiles) return MessageType.FILE;
        return MessageType.TEXT;
    }

    private String normalizedContent(String content) {
        return content == null || content.isBlank() ? null : content.strip();
    }

    private String requireClientMessageId(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank() || clientMessageId.strip().length() > 128) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "clientMessageId 不能为空且长度不能超过 128");
        }
        return clientMessageId.strip();
    }

    private void auditMessage(Long projectId,
                              ConversationModels.InternalMessageView message,
                              ActorRef actor,
                              String displayIdentity,
                              ProjectAuthorizationService.AuthorizationBasis basis) {
        auditLogWriter.append(new AuditLogWriter.Entry(
                projectId, actor, displayIdentity, auditSource(message.sendSource()), "MESSAGE", message.id(), null,
                message.correctionMessageId() == null ? "PROJECT_MESSAGE_SENT" : "PROJECT_MESSAGE_CORRECTION_SENT",
                objectMapper.convertValue(basis, Map.class), Map.of(),
                customerSafeMessageAuditState(message),
                AuditLogEntity.Result.SUCCESS, null, "message:" + message.clientMessageId(),
                "message:" + message.clientMessageId(), message.sentAt()));
    }

    private Map<String, Object> customerSafeMessageAuditState(
            ConversationModels.InternalMessageView message) {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("conversationId", message.conversationId());
        state.put("attachmentCount", message.attachments().size());
        if (message.correctionMessageId() != null) {
            state.put("correctsMessageId", message.correctionMessageId());
        }
        return Map.copyOf(state);
    }

    private AuditLogEntity.Source auditSource(MessageSendSource source) {
        return switch (source) {
            case CUSTOMER_UI -> AuditLogEntity.Source.CUSTOMER_UI;
            case DESIGNER_UI -> AuditLogEntity.Source.DESIGNER_UI;
            case ADMIN_UI -> AuditLogEntity.Source.ADMIN_UI;
            case AUTOMATION -> AuditLogEntity.Source.AUTOMATION;
            case EXTERNAL_EVENT -> AuditLogEntity.Source.EXTERNAL_EVENT;
            case SYSTEM -> AuditLogEntity.Source.SYSTEM;
        };
    }

    private String requireCustomerIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "客户显示身份不能为空");
        }
        return identity;
    }

    private ConversationEntity requireConversation(Long projectId) {
        return repository.findConversationByProjectId(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    private void requireProjectAccess(Long projectId) {
        if (!authorizationService.authorize(projectId, ProjectAuthorizationService.ProjectAction.VIEW_FULL).allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
