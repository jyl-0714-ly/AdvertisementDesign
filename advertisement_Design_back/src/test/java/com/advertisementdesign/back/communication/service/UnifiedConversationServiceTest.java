package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.ConversationStatus;
import com.advertisementdesign.back.communication.enums.ConversationType;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnifiedConversationServiceTest {

    @Test
    void consultationCreatesOneNullableProjectConversationAndUnifiedGreetings() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        AtomicLong messageIds = new AtomicLong();
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L)).thenReturn(Optional.empty());
        when(repository.saveConversation(any())).thenAnswer(invocation -> {
            ConversationEntity conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(100L);
            }
            return conversation;
        });
        when(repository.saveMessage(any())).thenAnswer(invocation -> {
            MessageEntity message = invocation.getArgument(0);
            message.setId(messageIds.incrementAndGet());
            return message;
        });
        UnifiedConversationService service = createService(repository);

        ConversationEntity conversation = service.ensureConsultationConversation(
                7L, 11L, 22L, List.of("您好", "请稍等"));

        assertEquals(100L, conversation.getId());
        assertEquals(7L, conversation.getConsultantIntakeId());
        assertNull(conversation.getProjectId());
        assertEquals(ConversationType.CONSULTATION, conversation.getConversationType());
        assertEquals(ConversationStatus.ACTIVE, conversation.getStatus());
        ArgumentCaptor<MessageEntity> messages = ArgumentCaptor.forClass(MessageEntity.class);
        verify(repository, times(2)).saveMessage(messages.capture());
        assertEquals(List.of("您好", "请稍等"), messages.getAllValues().stream()
                .map(MessageEntity::getContent).toList());
        assertEquals(List.of(MessageSenderRole.DESIGNER, MessageSenderRole.DESIGNER),
                messages.getAllValues().stream().map(MessageEntity::getSenderRole).toList());
    }

    @Test
    void repeatedConsultationCreationReusesExistingConversationWithoutMessages() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        ConversationEntity existing = consultationConversation();
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L))
                .thenReturn(Optional.of(existing));

        ConversationEntity result = createService(repository)
                .ensureConsultationConversation(7L, 11L, 22L, List.of("不会重复"));

        assertSame(existing, result);
        verify(repository, never()).saveMessage(any());
        verify(repository, never()).saveConversation(any());
    }

    @Test
    void concurrentConsultationCreationReusesWinningConversationWithoutDuplicateGreetings() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        ConversationEntity concurrent = consultationConversation();
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(repository.saveConversation(any()))
                .thenThrow(new DuplicateKeyException("duplicate intake conversation"));

        ConversationEntity result = createService(repository)
                .ensureConsultationConversation(7L, 11L, 22L, List.of("不会重复"));

        assertSame(concurrent, result);
        verify(repository, never()).saveMessage(any());
    }

    @Test
    void existingConversationCanAssignReplacementWithoutCopyingHistory() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        ConversationEntity existing = consultationConversation();
        AtomicLong messageIds = new AtomicLong();
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L))
                .thenReturn(Optional.of(existing));
        when(repository.saveConversation(existing)).thenReturn(existing);
        when(repository.saveMessage(any())).thenAnswer(invocation -> {
            MessageEntity message = invocation.getArgument(0);
            message.setId(messageIds.incrementAndGet());
            return message;
        });

        ConversationEntity result = createService(repository)
                .ensureConsultationConversation(
                        7L, 11L, 33L, List.of("由我继续为您服务"));

        assertSame(existing, result);
        assertEquals(33L, result.getDesignerId());
        verify(repository).saveConversation(existing);
        verify(repository).saveMessage(any());
        verify(repository, never()).listMessages(any());
    }

    @Test
    void reassignmentUpdatesDesignerAndAppendsGreetingsWithoutCopyingHistory() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        ConversationEntity existing = consultationConversation();
        AtomicLong messageIds = new AtomicLong();
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L))
                .thenReturn(Optional.of(existing));
        when(repository.saveConversation(existing)).thenReturn(existing);
        when(repository.saveMessage(any())).thenAnswer(invocation -> {
            MessageEntity message = invocation.getArgument(0);
            message.setId(messageIds.incrementAndGet());
            return message;
        });

        ConversationEntity result = createService(repository)
                .reassignConsultationDesigner(7L, 11L, 22L, 33L,
                        List.of("由我继续为您服务", "历史消息已保留"));

        assertSame(existing, result);
        assertEquals(33L, result.getDesignerId());
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(repository, times(2)).saveMessage(captor.capture());
        assertEquals(List.of("由我继续为您服务", "历史消息已保留"),
                captor.getAllValues().stream().map(MessageEntity::getContent).toList());
        assertEquals(List.of(33L, 33L),
                captor.getAllValues().stream().map(MessageEntity::getSenderId).toList());
        verify(repository, never()).listMessages(any());
    }

    @Test
    void reassignmentRejectsStalePreviousDesigner() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L))
                .thenReturn(Optional.of(consultationConversation()));

        ApiException exception = assertThrows(ApiException.class,
                () -> createService(repository).reassignConsultationDesigner(
                        7L, 11L, 44L, 33L, List.of()));

        assertEquals(403, exception.getCode());
        verify(repository, never()).saveMessage(any());
    }

    @Test
    void projectBindingUpdatesSameRowWithoutCopyingMessagesOrFiles() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        ConversationEntity existing = consultationConversation();
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L))
                .thenReturn(Optional.of(existing));
        when(repository.saveConversation(existing)).thenReturn(existing);

        ConversationEntity result = createService(repository)
                .bindProject(7L, 99L, 11L, 22L);

        assertSame(existing, result);
        assertEquals(99L, result.getProjectId());
        assertEquals(ConversationType.PROJECT, result.getConversationType());
        verify(repository).saveConversation(existing);
        verify(repository, never()).saveMessage(any());
    }

    @Test
    void bindingRejectsParticipantMismatchAndDifferentExistingProject() {
        CommunicationRepository repository = mock(CommunicationRepository.class);
        ConversationEntity existing = consultationConversation();
        when(repository.findConversationByConsultantIntakeIdForUpdate(7L))
                .thenReturn(Optional.of(existing));
        UnifiedConversationService service = createService(repository);

        assertEquals(403, assertThrows(ApiException.class,
                () -> service.bindProject(7L, 99L, 11L, 33L)).getCode());

        existing.setProjectId(88L);
        assertEquals(400, assertThrows(ApiException.class,
                () -> service.bindProject(7L, 99L, 11L, 22L)).getCode());
    }

    private UnifiedConversationService createService(CommunicationRepository repository) {
        ConversationAccessService accessService = mock(ConversationAccessService.class);
        org.mockito.Mockito.lenient().when(accessService.requireParticipants(any())).thenReturn(
                new ConversationAccessService.AuthoritativeParticipants(11L, 22L));
        return new UnifiedConversationService(repository, accessService);
    }

    private ConversationEntity consultationConversation() {
        return ConversationEntity.builder()
                .id(100L)
                .consultantIntakeId(7L)
                .projectId(null)
                .customerId(11L)
                .designerId(22L)
                .conversationType(ConversationType.CONSULTATION)
                .status(ConversationStatus.ACTIVE)
                .build();
    }
}
