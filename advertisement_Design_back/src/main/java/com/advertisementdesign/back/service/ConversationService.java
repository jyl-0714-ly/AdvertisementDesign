package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.conversation.ConversationModels;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.AuthContext;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.domain.entity.ConversationEntity;
import com.advertisementdesign.back.domain.entity.ConversationReadStateEntity;
import com.advertisementdesign.back.domain.entity.MessageEntity;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.MessageType;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final DemoDataStore store;
    private final ApiAssembler assembler;
    private final AuthService authService;

    public List<ConversationModels.ConversationVO> list() {
        UserEntity currentUser = authService.currentUserEntity();
        return store.listConversations().stream()
                .filter(conversation -> Objects.equals(conversation.getCustomerId(), currentUser.getId()) || Objects.equals(conversation.getDesignerId(), currentUser.getId()))
                .map(conversation -> assembler.toConversationVO(conversation, store.unreadCount(conversation.getId(), currentUser.getId())))
                .toList();
    }

    public ConversationModels.MessageCursorPage messages(Long conversationId, Long beforeMessageId, long size) {
        ConversationEntity conversation = findAllowedConversation(conversationId);
        List<MessageEntity> all = store.listMessages(conversation.getId());
        long pageSize = size <= 0 ? 20 : size;
        List<MessageEntity> filtered = all.stream()
                .filter(message -> beforeMessageId == null || message.getId() < beforeMessageId)
                .toList();
        int fromIndex = Math.max(filtered.size() - (int) pageSize, 0);
        List<ConversationModels.MessageVO> records = filtered.subList(fromIndex, filtered.size()).stream()
                .map(assembler::toMessageVO)
                .toList();
        boolean hasMore = fromIndex > 0;
        return new ConversationModels.MessageCursorPage(records, hasMore);
    }

    public ConversationModels.MessageVO sendMessage(Long conversationId, ConversationModels.SendMessageRequest request) {
        ConversationEntity conversation = findAllowedConversation(conversationId);
        CurrentUser currentUser = AuthContext.currentUser();
        UserEntity sender = authService.currentUserEntity();
        if (request.messageType() == null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        if (request.messageType() == MessageType.TEXT && (request.content() == null || request.content().isBlank())) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        MessageEntity message = MessageEntity.builder()
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .senderRole(MessageSenderRole.valueOf(sender.getRole().name()))
                .messageType(request.messageType())
                .content(request.content())
                .replyToMessageId(null)
                .clientMessageId(request.clientMessageId())
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .fileIds(request.fileIds())
                .build();
        message = store.saveMessage(message);
        conversation.setLastMessage(request.content() == null ? request.messageType().name() : request.content());
        conversation.setLastMessageAt(message.getCreatedAt());
        store.saveConversation(conversation);
        updateUnreadCount(conversation, currentUser.getId(), sender.getId(), message.getId());
        store.saveOperationLog(com.advertisementdesign.back.domain.entity.OperationLogEntity.builder()
                .operatorId(sender.getId())
                .operatorRole(MessageSenderRole.valueOf(sender.getRole().name()))
                .bizType("MESSAGE")
                .bizId(message.getId())
                .action("SEND")
                .description("发送消息")
                .afterData(java.util.Map.of("messageType", request.messageType().name()))
                .createdAt(LocalDateTime.now())
                .build());
        return assembler.toMessageVO(message);
    }

    public ConversationModels.ConversationReadStateVO markRead(Long conversationId, ConversationModels.MarkReadRequest request) {
        ConversationEntity conversation = findAllowedConversation(conversationId);
        UserEntity currentUser = authService.currentUserEntity();
        ConversationReadStateEntity readState = store.findReadState(conversation.getId(), currentUser.getId())
                .orElseGet(() -> ConversationReadStateEntity.builder()
                        .conversationId(conversation.getId())
                        .userId(currentUser.getId())
                        .unreadCount(0)
                        .build());
        readState.setLastReadMessageId(request.lastReadMessageId());
        readState.setLastReadAt(LocalDateTime.now());
        readState.setUnreadCount(0);
        store.saveReadState(readState);
        return assembler.toReadStateVO(readState);
    }

    public boolean deleteMessage(Long messageId) {
        MessageEntity message = store.findMessageById(messageId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserEntity currentUser = authService.currentUserEntity();
        if (message.getSenderId() != null && !message.getSenderId().equals(currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        message.setIsDeleted(true);
        store.saveMessage(message);
        return true;
    }

    private ConversationEntity findAllowedConversation(Long conversationId) {
        ConversationEntity conversation = store.findConversationById(conversationId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserEntity currentUser = authService.currentUserEntity();
        if (!Objects.equals(conversation.getCustomerId(), currentUser.getId()) && !Objects.equals(conversation.getDesignerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return conversation;
    }

    private void updateUnreadCount(ConversationEntity conversation, Long currentUserId, Long senderId, Long messageId) {
        Long otherUserId = Objects.equals(conversation.getCustomerId(), senderId) ? conversation.getDesignerId() : conversation.getCustomerId();
        ConversationReadStateEntity senderState = store.findReadState(conversation.getId(), currentUserId)
                .orElseGet(() -> ConversationReadStateEntity.builder()
                        .conversationId(conversation.getId())
                        .userId(currentUserId)
                        .build());
        senderState.setLastReadMessageId(messageId);
        senderState.setLastReadAt(LocalDateTime.now());
        senderState.setUnreadCount(0);
        store.saveReadState(senderState);

        ConversationReadStateEntity otherState = store.findReadState(conversation.getId(), otherUserId)
                .orElseGet(() -> ConversationReadStateEntity.builder()
                        .conversationId(conversation.getId())
                        .userId(otherUserId)
                        .build());
        otherState.setUnreadCount(Optional.ofNullable(otherState.getUnreadCount()).orElse(0) + 1);
        store.saveReadState(otherState);
    }
}
