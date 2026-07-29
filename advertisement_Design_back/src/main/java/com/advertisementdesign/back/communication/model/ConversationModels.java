package com.advertisementdesign.back.communication.model;

import com.advertisementdesign.back.communication.enums.ConversationStatus;
import com.advertisementdesign.back.communication.enums.MessageSendSource;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "项目会话公开契约")
public final class ConversationModels {
    public static final String SERVICE_TEAM_IDENTITY = "项目服务团队";

    private ConversationModels() {
    }

    public record ConversationView(
            Long id,
            Long projectId,
            ConversationStatus status,
            Long lastMessageId,
            String lastMessagePreview,
            LocalDateTime lastMessageAt,
            Long version
    ) {
    }

    /** Customer-facing view intentionally excludes real internal actor identity. */
    public record CustomerMessageView(
            Long id,
            Long conversationId,
            MessageType messageType,
            String content,
            String displayIdentity,
            Long replyToMessageId,
            Long correctionMessageId,
            List<AttachmentView> attachments,
            LocalDateTime sentAt
    ) {
        public CustomerMessageView {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
        }
    }

    public record InternalMessageView(
            Long id,
            Long conversationId,
            MessageType messageType,
            String content,
            String customerDisplayIdentity,
            ActorRef actor,
            MessageSendSource sendSource,
            String authorizationBasis,
            Long replyToMessageId,
            Long correctionMessageId,
            String clientMessageId,
            List<AttachmentView> attachments,
            LocalDateTime sentAt
    ) {
        public InternalMessageView {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
        }
    }

    public record AttachmentView(Long id, Long fileAssetId, Integer displayOrder, LocalDateTime createdAt) {
    }

    public record ReadStateView(
            Long conversationId,
            Long userId,
            Long lastReadMessageId,
            LocalDateTime lastReadAt,
            Integer unreadCount,
            Long version
    ) {
    }

    public record CurrentUserAppendCommand(
            Long projectId,
            MessageType messageType,
            String content,
            Long replyToMessageId,
            Long correctionMessageId,
            String clientMessageId,
            List<Long> fileAssetIds
    ) {
        public CurrentUserAppendCommand {
            fileAssetIds = fileAssetIds == null ? List.of() : List.copyOf(fileAssetIds);
        }
    }

    public record TrustedInternalAppendCommand(
            Long projectId,
            MessageType messageType,
            String content,
            String customerDisplayIdentity,
            ActorRef actor,
            MessageSendSource sendSource,
            ProjectAuthorizationService.AuthorizationBasis authorizationBasis,
            Long replyToMessageId,
            Long correctionMessageId,
            String clientMessageId,
            List<Long> fileAssetIds,
            LocalDateTime sentAt
    ) {
        public TrustedInternalAppendCommand {
            fileAssetIds = fileAssetIds == null ? List.of() : List.copyOf(fileAssetIds);
        }
    }
}
