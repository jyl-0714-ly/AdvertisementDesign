package com.advertisementdesign.back.communication.converter;

import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.ConversationReadStateEntity;
import com.advertisementdesign.back.communication.entity.MessageAttachmentEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.identity.model.ActorRef;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationConverter {
    public ConversationModels.ConversationView toConversation(ConversationEntity entity) {
        return new ConversationModels.ConversationView(
                entity.getId(), entity.getProjectId(), entity.getStatus(), entity.getLastMessageId(),
                entity.getLastMessagePreview(), entity.getLastMessageAt(), entity.getVersion());
    }

    public ConversationModels.CustomerMessageView toCustomerMessage(
            MessageEntity entity, List<MessageAttachmentEntity> attachments) {
        return new ConversationModels.CustomerMessageView(
                entity.getId(), entity.getConversationId(), entity.getMessageType(), entity.getContent(),
                entity.getCustomerDisplayIdentity(), entity.getReplyToMessageId(), entity.getCorrectionMessageId(),
                attachments.stream().map(this::toAttachment).toList(), entity.getSentAt());
    }

    public ConversationModels.CustomerMessageView toCustomerMessageView(
            MessageEntity entity, List<ConversationModels.AttachmentView> attachments) {
        return new ConversationModels.CustomerMessageView(
                entity.getId(), entity.getConversationId(), entity.getMessageType(), entity.getContent(),
                entity.getCustomerDisplayIdentity(), entity.getReplyToMessageId(), entity.getCorrectionMessageId(),
                attachments, entity.getSentAt());
    }

    public ConversationModels.InternalMessageView toInternalMessage(
            MessageEntity entity, List<MessageAttachmentEntity> attachments) {
        return new ConversationModels.InternalMessageView(
                entity.getId(), entity.getConversationId(), entity.getMessageType(), entity.getContent(),
                entity.getCustomerDisplayIdentity(), new ActorRef(entity.getActorType(), entity.getActorId()),
                entity.getSendSource(), entity.getAuthorizationBasis(), entity.getReplyToMessageId(),
                entity.getCorrectionMessageId(), entity.getClientMessageId(),
                attachments.stream().map(this::toAttachment).toList(), entity.getSentAt());
    }

    public ConversationModels.ReadStateView toReadState(ConversationReadStateEntity entity) {
        return new ConversationModels.ReadStateView(
                entity.getConversationId(), entity.getUserId(), entity.getLastReadMessageId(),
                entity.getLastReadAt(), entity.getUnreadCount(), entity.getVersion());
    }

    private ConversationModels.AttachmentView toAttachment(MessageAttachmentEntity entity) {
        return new ConversationModels.AttachmentView(
                entity.getId(), entity.getFileAssetId(), entity.getDisplayOrder(),
                null, null, null, null, entity.getCreatedAt());
    }
}
