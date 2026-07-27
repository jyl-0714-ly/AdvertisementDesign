package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.communication.converter.ConversationConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.common.audit.entity.OperationLogEntity;
import com.advertisementdesign.back.common.audit.repository.AuditRepository;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.ConversationReadStateEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.communication.enums.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final CommunicationRepository communicationRepository;
    private final StorageRepository storageRepository;
    private final ProjectRepository projectRepository;
    private final AuditRepository auditRepository;
    private final ConversationConverter converter;
    private final AuthService authService;

    public List<ConversationModels.ConversationVO> list() {
        UserProfile currentUser = authService.currentUserProfile();
        return communicationRepository.listConversations().stream()
                .filter(conversation -> Objects.equals(conversation.getCustomerId(), currentUser.id()) || Objects.equals(conversation.getDesignerId(), currentUser.id()))
                .map(conversation -> converter.toConversationVO(conversation, communicationRepository.unreadCount(conversation.getId(), currentUser.id())))
                .toList();
    }

    public ConversationModels.MessageCursorPage messages(Long conversationId, Long beforeMessageId, long size) {
        ConversationEntity conversation = findAllowedConversation(conversationId);
        List<MessageEntity> all = communicationRepository.listMessages(conversation.getId());
        long requestedSize = size <= 0 ? 20 : size;
        int pageSize = (int) Math.min(requestedSize, Integer.MAX_VALUE);
        List<MessageEntity> filtered = all.stream()
                .filter(message -> beforeMessageId == null || message.getId() < beforeMessageId)
                .toList();
        int fromIndex = Math.max(filtered.size() - pageSize, 0);
        List<ConversationModels.MessageVO> records = filtered.subList(fromIndex, filtered.size()).stream()
                .map(converter::toMessageVO)
                .toList();
        boolean hasMore = fromIndex > 0;
        return new ConversationModels.MessageCursorPage(records, hasMore);
    }

    @Transactional
    public ConversationModels.MessageVO sendMessage(Long conversationId, ConversationModels.SendMessageRequest request) {
        ConversationEntity conversation = findAllowedConversationForUpdate(conversationId);
        UserProfile sender = authService.currentUserProfile();
        if (request.messageType() == null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        if (request.messageType() == MessageType.TEXT && (request.content() == null || request.content().isBlank())) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        List<Long> fileIds = request.fileIds() == null ? List.of() : request.fileIds();
        for (Long fileId : fileIds) {
            FileAssetEntity file = storageRepository.findById(fileId)
                    .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
            if (file.getStatus() == FileStatus.DELETED) {
                throw new ApiException(ApiErrorCode.NOT_FOUND);
            }
            if (!Objects.equals(file.getUploaderId(), sender.id())) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
        }
        MessageEntity message = MessageEntity.builder()
                .conversationId(conversation.getId())
                .senderId(sender.id())
                .senderRole(MessageSenderRole.valueOf(sender.role().name()))
                .messageType(request.messageType())
                .content(request.content())
                .replyToMessageId(null)
                .clientMessageId(request.clientMessageId())
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .fileIds(fileIds)
                .build();
        message = communicationRepository.saveMessage(message);
        conversation.setLastMessage(request.content() == null ? request.messageType().name() : request.content());
        conversation.setLastMessageAt(message.getCreatedAt());
        communicationRepository.saveConversation(conversation);
        updateUnreadCount(conversation, sender.id(), message.getId());
        auditRepository.save(OperationLogEntity.builder()
                .operatorId(sender.id())
                .operatorRole(MessageSenderRole.valueOf(sender.role().name()))
                .bizType("MESSAGE")
                .bizId(message.getId())
                .action("SEND")
                .description("发送消息")
                .afterData(java.util.Map.of("messageType", request.messageType().name()))
                .createdAt(LocalDateTime.now())
                .build());
        return converter.toMessageVO(message);
    }

    @Transactional
    public ConversationModels.ConversationReadStateVO markRead(Long conversationId, ConversationModels.MarkReadRequest request) {
        ConversationEntity conversation = findAllowedConversationForUpdate(conversationId);
        MessageEntity lastReadMessage = communicationRepository.findMessageById(request.lastReadMessageId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!Objects.equals(lastReadMessage.getConversationId(), conversation.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        UserProfile currentUser = authService.currentUserProfile();
        ConversationReadStateEntity readState = communicationRepository.resetReadState(
                conversation.getId(), currentUser.id(), request.lastReadMessageId());
        return converter.toReadStateVO(readState);
    }

    @Transactional
    public boolean deleteMessage(Long messageId) {
        MessageEntity message = communicationRepository.findMessageById(messageId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ConversationEntity conversation = findAllowedConversationForUpdate(message.getConversationId());
        UserProfile currentUser = authService.currentUserProfile();
        if (message.getSenderId() == null || !message.getSenderId().equals(currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        message.setIsDeleted(true);
        communicationRepository.saveMessage(message);
        MessageEntity latestMessage = communicationRepository.findLatestActiveMessage(conversation.getId()).orElse(null);
        conversation.setLastMessage(latestMessage == null
                ? null
                : latestMessage.getContent() == null
                        ? latestMessage.getMessageType().name()
                        : latestMessage.getContent());
        conversation.setLastMessageAt(latestMessage == null ? null : latestMessage.getCreatedAt());
        communicationRepository.saveConversation(conversation);
        communicationRepository.refreshUnreadCount(message.getConversationId(), currentUser.id());
        Long otherUserId = Objects.equals(conversation.getCustomerId(), currentUser.id())
                ? conversation.getDesignerId()
                : conversation.getCustomerId();
        communicationRepository.refreshUnreadCount(message.getConversationId(), otherUserId);
        return true;
    }

    private ConversationEntity findAllowedConversation(Long conversationId) {
        ConversationEntity conversation = communicationRepository.findConversationById(conversationId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        validateParticipant(conversation);
        return conversation;
    }

    private ConversationEntity findAllowedConversationForUpdate(Long conversationId) {
        ConversationEntity conversation = communicationRepository.findConversationByIdForUpdate(conversationId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        validateParticipant(conversation);
        return conversation;
    }

    private void validateParticipant(ConversationEntity conversation) {
        UserProfile currentUser = authService.currentUserProfile();
        if (!Objects.equals(conversation.getCustomerId(), currentUser.id()) && !Objects.equals(conversation.getDesignerId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private void updateUnreadCount(ConversationEntity conversation, Long senderId, Long messageId) {
        Long otherUserId = Objects.equals(conversation.getCustomerId(), senderId) ? conversation.getDesignerId() : conversation.getCustomerId();
        communicationRepository.resetReadState(
                conversation.getId(), senderId, messageId);
        communicationRepository.incrementUnreadCount(conversation.getId(), otherUserId);
    }
}
