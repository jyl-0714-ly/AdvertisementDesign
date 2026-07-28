package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.ConversationStatus;
import com.advertisementdesign.back.communication.enums.ConversationType;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 统一会话的跨业务应用边界。咨询和项目模块只通过该服务操作会话，
 * 不直接访问 communication 模块的 Mapper，也不复制消息或附件关系。
 */
@Service
@RequiredArgsConstructor
public class UnifiedConversationService {
    private final CommunicationRepository communicationRepository;
    private final ConversationAccessService conversationAccessService;

    @Transactional
    public ConversationEntity ensureConsultationConversation(
            Long consultantIntakeId,
            Long customerId,
            Long designerId,
            List<String> designerGreetings) {
        ConversationEntity existing = communicationRepository
                .findConversationByConsultantIntakeIdForUpdate(consultantIntakeId)
                .orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getCustomerId(), customerId)) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
            if (!Objects.equals(existing.getDesignerId(), designerId)) {
                existing.setDesignerId(designerId);
                existing.setUpdatedAt(LocalDateTime.now());
                communicationRepository.saveConversation(existing);
                appendDesignerGreetings(existing, designerId, designerGreetings);
            }
            return existing;
        }

        ConversationEntity conversation = ConversationEntity.builder()
                .consultantIntakeId(consultantIntakeId)
                .projectId(null)
                .customerId(customerId)
                .designerId(designerId)
                .conversationType(ConversationType.CONSULTATION)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            communicationRepository.saveConversation(conversation);
        } catch (DuplicateKeyException exception) {
            ConversationEntity concurrent = communicationRepository
                    .findConversationByConsultantIntakeIdForUpdate(consultantIntakeId)
                    .orElseThrow(() -> exception);
            validateParticipants(concurrent, customerId, designerId);
            return concurrent;
        }

        appendDesignerGreetings(conversation, designerId, designerGreetings);
        return conversation;
    }

    public Long findConversationIdByConsultantIntakeId(Long consultantIntakeId) {
        return communicationRepository.findConversationByConsultantIntakeId(consultantIntakeId)
                .map(ConversationEntity::getId)
                .orElse(null);
    }

    public ConversationEntity findByConsultantIntakeId(Long consultantIntakeId) {
        return communicationRepository.findConversationByConsultantIntakeId(consultantIntakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    public List<MessageEntity> listMessagesByConsultantIntakeId(Long consultantIntakeId) {
        ConversationEntity conversation = findByConsultantIntakeId(consultantIntakeId);
        return communicationRepository.listMessages(conversation.getId());
    }

    @Transactional
    public MessageEntity appendHumanMessage(
            Long consultantIntakeId,
            Long senderId,
            MessageSenderRole senderRole,
            String content) {
        ConversationEntity conversation = communicationRepository
                .findConversationByConsultantIntakeIdForUpdate(consultantIntakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        return appendMessage(conversation, senderId, senderRole, content.trim());
    }

    @Transactional
    public ConversationEntity reassignConsultationDesigner(
            Long consultantIntakeId,
            Long customerId,
            Long previousDesignerId,
            Long newDesignerId,
            List<String> designerGreetings) {
        ConversationEntity conversation = communicationRepository
                .findConversationByConsultantIntakeIdForUpdate(consultantIntakeId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!Objects.equals(conversation.getCustomerId(), customerId)
                || !Objects.equals(conversation.getDesignerId(), previousDesignerId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        conversation.setDesignerId(newDesignerId);
        conversation.setUpdatedAt(LocalDateTime.now());
        communicationRepository.saveConversation(conversation);
        if (designerGreetings != null) {
            designerGreetings.stream()
                    .filter(content -> content != null && !content.isBlank())
                    .forEach(content -> appendMessage(
                            conversation, newDesignerId,
                            MessageSenderRole.DESIGNER, content.trim()));
        }
        return conversation;
    }

    @Transactional
    public ConversationEntity bindProject(
            Long consultantIntakeId,
            Long projectId,
            Long customerId,
            Long designerId) {
        ConversationEntity conversation = communicationRepository
                .findConversationByConsultantIntakeIdForUpdate(consultantIntakeId)
                .orElse(null);
        if (conversation == null) {
            conversation = ConversationEntity.builder()
                    .consultantIntakeId(consultantIntakeId)
                    .projectId(projectId)
                    .customerId(customerId)
                    .designerId(designerId)
                    .conversationType(ConversationType.PROJECT)
                    .status(ConversationStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            try {
                communicationRepository.saveConversation(conversation);
                return conversation;
            } catch (DuplicateKeyException exception) {
                ConversationEntity concurrent = communicationRepository
                        .findConversationByConsultantIntakeIdForUpdate(consultantIntakeId)
                        .orElseThrow(() -> exception);
                validateParticipants(concurrent, customerId, designerId);
                if (concurrent.getProjectId() != null
                        && !Objects.equals(concurrent.getProjectId(), projectId)) {
                    throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "该咨询会话已绑定其他项目");
                }
                concurrent.setProjectId(projectId);
                concurrent.setConversationType(ConversationType.PROJECT);
                concurrent.setStatus(ConversationStatus.ACTIVE);
                return communicationRepository.saveConversation(concurrent);
            }
        }
        validateParticipants(conversation, customerId, designerId);
        if (conversation.getProjectId() != null
                && !Objects.equals(conversation.getProjectId(), projectId)) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "该咨询会话已绑定其他项目");
        }
        conversation.setProjectId(projectId);
        conversation.setConversationType(ConversationType.PROJECT);
        conversation.setStatus(ConversationStatus.ACTIVE);
        return communicationRepository.saveConversation(conversation);
    }

    private void appendDesignerGreetings(
            ConversationEntity conversation,
            Long designerId,
            List<String> designerGreetings) {
        if (designerGreetings == null) {
            return;
        }
        designerGreetings.stream()
                .filter(content -> content != null && !content.isBlank())
                .forEach(content -> appendMessage(
                        conversation,
                        designerId,
                        MessageSenderRole.DESIGNER,
                        content.trim()));
    }

    private MessageEntity appendMessage(
            ConversationEntity conversation,
            Long senderId,
            MessageSenderRole senderRole,
            String content) {
        LocalDateTime now = LocalDateTime.now();
        MessageEntity message = communicationRepository.saveMessage(MessageEntity.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .senderRole(senderRole)
                .messageType(MessageType.TEXT)
                .content(content)
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .fileIds(List.of())
                .build());
        conversation.setLastMessage(content);
        conversation.setLastMessageAt(message.getCreatedAt());
        communicationRepository.saveConversation(conversation);
        communicationRepository.resetReadState(conversation.getId(), senderId, message.getId());
        ConversationAccessService.AuthoritativeParticipants participants =
                conversationAccessService.requireParticipants(conversation);
        Long recipientId = Objects.equals(senderId, participants.customerId())
                ? participants.designerId()
                : participants.customerId();
        communicationRepository.incrementUnreadCount(conversation.getId(), recipientId);
        return message;
    }

    private void validateParticipants(
            ConversationEntity conversation,
            Long customerId,
            Long designerId) {
        if (!Objects.equals(conversation.getCustomerId(), customerId)
                || !Objects.equals(conversation.getDesignerId(), designerId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
