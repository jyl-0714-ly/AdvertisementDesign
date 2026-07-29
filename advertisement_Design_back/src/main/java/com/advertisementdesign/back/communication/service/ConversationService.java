package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.converter.ConversationConverter;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.MessageSendSource;
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
    private final ObjectMapper objectMapper;

    public ConversationModels.ConversationView conversation(Long projectId) {
        requireProjectAccess(projectId);
        return converter.toConversation(requireConversation(projectId));
    }

    public List<ConversationModels.CustomerMessageView> customerMessages(
            Long projectId, Long beforeMessageId, long size) {
        requireProjectAccess(projectId);
        ConversationEntity conversation = requireConversation(projectId);
        return repository.listMessages(conversation.getId(), beforeMessageId, size).stream()
                .map(message -> converter.toCustomerMessage(message, repository.listAttachments(message.getId())))
                .toList();
    }

    /**
     * Appends on behalf of the authenticated user. Actor, source, display identity and authorization
     * evidence are derived here and cannot be asserted by the caller.
     */
    @Transactional
    public ConversationModels.InternalMessageView appendAsCurrentUser(
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
        return appendTrustedInternal(new ConversationModels.TrustedInternalAppendCommand(
                command.projectId(), command.messageType(), command.content(), displayIdentity,
                currentActor.actor(), sourceFor(currentActor.actor()), decision.basis(), command.replyToMessageId(),
                command.correctionMessageId(), command.clientMessageId(), command.fileAssetIds(), null));
    }

    /**
     * Trusted internal append-only boundary for future atomic project/conversation orchestration.
     * It accepts pre-established actor and authorization evidence and is deliberately not a controller contract.
     * Attachments require a separate validated attachment command and are rejected by this foundation boundary.
     */
    @Transactional
    public ConversationModels.InternalMessageView appendTrustedInternal(
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
        if (!command.fileAssetIds().isEmpty()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(),
                    "附件必须通过已校验草稿所有权、文件状态和项目关系的附件命令发送");
        }
        if (command.content() == null || command.content().isBlank()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "消息正文不能为空");
        }
        validateTarget(conversation.getId(), command.replyToMessageId());
        validateTarget(conversation.getId(), command.correctionMessageId());
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
