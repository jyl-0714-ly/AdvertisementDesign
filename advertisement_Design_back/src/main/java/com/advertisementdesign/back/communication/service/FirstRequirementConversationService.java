package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.ConversationStatus;
import com.advertisementdesign.back.communication.enums.MessageSendSource;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.model.ActorRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FirstRequirementConversationService {
    private final CommunicationRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public CreatedConversation initialize(Command command) {
        LocalDateTime now = command.occurredAt();
        ConversationEntity conversation = repository.createConversation(ConversationEntity.builder()
                .projectId(command.projectId())
                .status(ConversationStatus.ACTIVE)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build());
        MessageEntity message = repository.appendMessage(MessageEntity.builder()
                .conversationId(conversation.getId())
                .messageType(command.fileAssetIds().isEmpty() ? MessageType.TEXT : MessageType.MIXED)
                .content(command.content())
                .customerDisplayIdentity(command.customerDisplayIdentity())
                .actorType(ActorRef.ActorType.CUSTOMER_USER)
                .actorId(command.actorId())
                .sendSource(MessageSendSource.CUSTOMER_UI)
                .authorizationBasis(command.authorizationBasis())
                .clientMessageId(command.clientMessageId())
                .sentAt(now)
                .build(), command.fileAssetIds());
        repository.updateLastMessage(conversation, message);
        return new CreatedConversation(conversation.getId(), message.getId());
    }

    public record Command(Long projectId, Long actorId, String customerDisplayIdentity,
                          String content, String authorizationBasis, String clientMessageId,
                          List<Long> fileAssetIds, LocalDateTime occurredAt) {
    }

    public record CreatedConversation(Long conversationId, Long messageId) {
    }
}
