package com.advertisementdesign.back.communication.repository;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.ConversationReadStateEntity;
import com.advertisementdesign.back.communication.entity.MessageAttachmentEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.mapper.ConversationMapper;
import com.advertisementdesign.back.communication.mapper.ConversationReadStateMapper;
import com.advertisementdesign.back.communication.mapper.MessageAttachmentMapper;
import com.advertisementdesign.back.communication.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommunicationRepository {
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final MessageAttachmentMapper attachmentMapper;
    private final ConversationReadStateMapper readStateMapper;

    public ConversationEntity createConversation(ConversationEntity conversation) {
        if (conversation.getId() != null) {
            throw new IllegalArgumentException("New conversation must not have an id");
        }
        requireInserted(conversationMapper.insert(conversation));
        return conversation;
    }

    public Optional<ConversationEntity> findConversationByProjectId(Long projectId) {
        return Optional.ofNullable(conversationMapper.selectOne(new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getProjectId, projectId).last("LIMIT 1")));
    }

    public Optional<ConversationEntity> findConversationById(Long conversationId) {
        return Optional.ofNullable(conversationMapper.selectById(conversationId));
    }

    public List<MessageEntity> listMessages(Long conversationId, Long beforeMessageId, long size) {
        LambdaQueryWrapper<MessageEntity> query = new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getConversationId, conversationId)
                .orderByDesc(MessageEntity::getSentAt)
                .orderByDesc(MessageEntity::getId)
                .last("LIMIT " + Math.max(1, Math.min(size, 100)));
        if (beforeMessageId != null) query.lt(MessageEntity::getId, beforeMessageId);
        return messageMapper.selectList(query);
    }

    public Optional<MessageEntity> findMessage(Long conversationId, Long messageId) {
        return Optional.ofNullable(messageMapper.selectOne(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getConversationId, conversationId)
                .eq(MessageEntity::getId, messageId).last("LIMIT 1")));
    }

    public MessageEntity appendMessage(MessageEntity message, List<Long> fileAssetIds) {
        if (message.getId() != null) {
            throw new IllegalArgumentException("Messages are append-only");
        }
        if (message.getSentAt() == null) message.setSentAt(LocalDateTime.now());
        requireInserted(messageMapper.insert(message));
        int order = 0;
        for (Long fileAssetId : fileAssetIds == null ? List.<Long>of() : fileAssetIds.stream().distinct().toList()) {
            MessageAttachmentEntity attachment = MessageAttachmentEntity.builder()
                    .messageId(message.getId()).fileAssetId(fileAssetId)
                    .displayOrder(order++).createdAt(message.getSentAt()).build();
            requireInserted(attachmentMapper.insert(attachment));
        }
        return message;
    }

    public ConversationEntity updateLastMessage(ConversationEntity conversation, MessageEntity message) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessagePreview(preview(message.getContent()));
        conversation.setLastMessageAt(message.getSentAt());
        conversation.setUpdatedAt(message.getSentAt());
        if (conversationMapper.updateById(conversation) != 1) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "项目会话已被其他消息更新，请重试");
        }
        return conversation;
    }

    public List<MessageAttachmentEntity> listAttachments(Long messageId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<MessageAttachmentEntity>()
                .eq(MessageAttachmentEntity::getMessageId, messageId)
                .orderByAsc(MessageAttachmentEntity::getDisplayOrder));
    }

    public Optional<ConversationEntity> findConversationByAttachedFileId(Long fileAssetId) {
        MessageAttachmentEntity attachment = attachmentMapper.selectOne(
                new LambdaQueryWrapper<MessageAttachmentEntity>()
                        .eq(MessageAttachmentEntity::getFileAssetId, fileAssetId).last("LIMIT 1"));
        if (attachment == null) return Optional.empty();
        MessageEntity message = messageMapper.selectById(attachment.getMessageId());
        return message == null ? Optional.empty() : findConversationById(message.getConversationId());
    }

    public Optional<ConversationReadStateEntity> findReadState(Long conversationId, Long userId) {
        return Optional.ofNullable(readStateMapper.selectOne(new LambdaQueryWrapper<ConversationReadStateEntity>()
                .eq(ConversationReadStateEntity::getConversationId, conversationId)
                .eq(ConversationReadStateEntity::getUserId, userId).last("LIMIT 1")));
    }

    public ConversationReadStateEntity saveReadState(ConversationReadStateEntity state) {
        state.setUpdatedAt(LocalDateTime.now());
        int affected = state.getId() == null ? readStateMapper.insert(state) : readStateMapper.updateById(state);
        if (affected != 1) throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "已读状态已更新，请刷新后重试");
        return state;
    }

    private String preview(String content) {
        if (content == null || content.isBlank()) return "[附件]";
        String normalized = content.strip().replaceAll("\\s+", " ");
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private void requireInserted(int affected) {
        if (affected != 1) throw new ApiException(ApiErrorCode.CONFLICT);
    }
}
