package com.advertisementdesign.back.communication.repository;

import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.ConversationReadStateEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.entity.MessageFileEntity;
import com.advertisementdesign.back.communication.mapper.ConversationMapper;
import com.advertisementdesign.back.communication.mapper.ConversationReadStateMapper;
import com.advertisementdesign.back.communication.mapper.MessageFileMapper;
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
    private final MessageFileMapper messageFileMapper;
    private final ConversationReadStateMapper readStateMapper;

    public List<ConversationEntity> listConversations() {
        return conversationMapper.selectList(new LambdaQueryWrapper<ConversationEntity>()
                .orderByDesc(ConversationEntity::getLastMessageAt));
    }

    public Optional<ConversationEntity> findConversationById(Long id) {
        return Optional.ofNullable(conversationMapper.selectById(id));
    }

    public Optional<ConversationEntity> findConversationByIdForUpdate(Long id) {
        return Optional.ofNullable(conversationMapper.selectByIdForUpdate(id));
    }

    public Optional<ConversationEntity> findConversationByProjectId(Long projectId) {
        return Optional.ofNullable(conversationMapper.selectOne(new LambdaQueryWrapper<ConversationEntity>()
                .eq(ConversationEntity::getProjectId, projectId)));
    }

    public Optional<ConversationEntity> findConversationByAttachedFileId(Long fileId) {
        return Optional.ofNullable(conversationMapper.selectByAttachedFileId(fileId));
    }

    public ConversationEntity saveConversation(ConversationEntity conversation) {
        LocalDateTime now = LocalDateTime.now();
        if (conversation.getId() == null) {
            if (conversation.getCreatedAt() == null) {
                conversation.setCreatedAt(now);
            }
            conversation.setUpdatedAt(now);
            conversationMapper.insert(conversation);
        } else {
            conversation.setUpdatedAt(now);
            conversationMapper.updateById(conversation);
        }
        return conversation;
    }

    public List<MessageEntity> listMessages(Long conversationId) {
        List<MessageEntity> messages = messageMapper.selectList(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getConversationId, conversationId)
                .eq(MessageEntity::getIsDeleted, false)
                .orderByAsc(MessageEntity::getCreatedAt));
        messages.forEach(this::loadFileIds);
        return messages;
    }

    public Optional<MessageEntity> findMessageById(Long id) {
        MessageEntity message = messageMapper.selectById(id);
        if (message != null) {
            loadFileIds(message);
        }
        return Optional.ofNullable(message);
    }

    public Optional<MessageEntity> findLatestActiveMessage(Long conversationId) {
        return Optional.ofNullable(messageMapper.selectLatestActiveByConversationId(conversationId));
    }

    public MessageEntity saveMessage(MessageEntity message) {
        LocalDateTime now = LocalDateTime.now();
        if (message.getId() == null) {
            if (message.getCreatedAt() == null) {
                message.setCreatedAt(now);
            }
            message.setUpdatedAt(now);
            messageMapper.insert(message);
        } else {
            message.setUpdatedAt(now);
            messageMapper.updateById(message);
        }
        replaceMessageFiles(message.getId(), message.getFileIds());
        return message;
    }

    public Optional<ConversationReadStateEntity> findReadState(Long conversationId, Long userId) {
        return Optional.ofNullable(readStateMapper.selectOne(
                new LambdaQueryWrapper<ConversationReadStateEntity>()
                        .eq(ConversationReadStateEntity::getConversationId, conversationId)
                        .eq(ConversationReadStateEntity::getUserId, userId)));
    }

    public ConversationReadStateEntity saveReadState(ConversationReadStateEntity readState) {
        readState.setUpdatedAt(LocalDateTime.now());
        if (readState.getId() == null) {
            readStateMapper.insert(readState);
        } else {
            readStateMapper.updateById(readState);
        }
        return readState;
    }

    public ConversationReadStateEntity resetReadState(
            Long conversationId, Long userId, Long lastReadMessageId) {
        LocalDateTime now = LocalDateTime.now();
        readStateMapper.resetReadState(conversationId, userId, lastReadMessageId, now, now);
        return findReadState(conversationId, userId)
                .orElseThrow(() -> new IllegalStateException("Read state upsert failed"));
    }

    public void incrementUnreadCount(Long conversationId, Long userId) {
        readStateMapper.incrementUnreadCount(conversationId, userId, LocalDateTime.now());
    }

    public void refreshUnreadCount(Long conversationId, Long userId) {
        readStateMapper.refreshUnreadCount(conversationId, userId, LocalDateTime.now());
    }

    public int unreadCount(Long conversationId, Long userId) {
        return findReadState(conversationId, userId)
                .map(ConversationReadStateEntity::getUnreadCount)
                .orElse(0);
    }

    public boolean isFileAttachedToConversation(Long fileId, Long conversationId) {
        return messageFileMapper.countByFileAndConversation(fileId, conversationId) > 0;
    }

    public boolean canUserAccessAttachedFile(Long fileId, Long userId) {
        return messageFileMapper.countAccessibleByUser(fileId, userId) > 0;
    }

    private void loadFileIds(MessageEntity message) {
        List<Long> fileIds = messageFileMapper.selectList(new LambdaQueryWrapper<MessageFileEntity>()
                        .eq(MessageFileEntity::getMessageId, message.getId())
                        .orderByAsc(MessageFileEntity::getId))
                .stream()
                .map(MessageFileEntity::getFileId)
                .toList();
        message.setFileIds(fileIds);
    }

    private void replaceMessageFiles(Long messageId, List<Long> fileIds) {
        messageFileMapper.delete(new LambdaQueryWrapper<MessageFileEntity>()
                .eq(MessageFileEntity::getMessageId, messageId));
        if (fileIds == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        fileIds.stream().distinct().forEach(fileId -> messageFileMapper.insert(MessageFileEntity.builder()
                .messageId(messageId)
                .fileId(fileId)
                .createdAt(now)
                .build()));
    }
}
