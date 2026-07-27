package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.communication.converter.ConversationConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.audit.repository.AuditRepository;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    @Mock
    private CommunicationRepository communicationRepository;
    @Mock
    private StorageRepository storageRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AuditRepository auditRepository;
    @Mock
    private ConversationConverter converter;
    @Mock
    private AuthService authService;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                communicationRepository, storageRepository, projectRepository, auditRepository, converter, authService);
    }

    @Test
    void sendMessageDefinesTransactionBoundary() throws NoSuchMethodException {
        Method method = ConversationService.class.getMethod(
                "sendMessage", Long.class, ConversationModels.SendMessageRequest.class);

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    void markReadDefinesTransactionBoundary() throws NoSuchMethodException {
        Method method = ConversationService.class.getMethod(
                "markRead", Long.class, ConversationModels.MarkReadRequest.class);

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    void deleteMessageDefinesTransactionBoundary() throws NoSuchMethodException {
        Method method = ConversationService.class.getMethod("deleteMessage", Long.class);

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    void nonMemberCannotListConversationMessages() {
        when(communicationRepository.findConversationById(1L)).thenReturn(Optional.of(conversation()));
        when(authService.currentUserProfile()).thenReturn(user(99L, UserRole.CUSTOMER));

        ApiException exception = assertThrows(ApiException.class,
                () -> conversationService.messages(1L, null, 20));

        assertEquals(403, exception.getCode());
        verify(communicationRepository, never()).listMessages(any());
    }

    @Test
    void memberCannotAttachFileUploadedByAnotherUser() {
        when(communicationRepository.findConversationByIdForUpdate(1L)).thenReturn(Optional.of(conversation()));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));
        when(storageRepository.findById(8L)).thenReturn(Optional.of(FileAssetEntity.builder()
                .id(8L)
                .uploaderId(2L)
                .status(FileStatus.ACTIVE)
                .build()));

        ApiException exception = assertThrows(ApiException.class,
                () -> conversationService.sendMessage(1L,
                        new ConversationModels.SendMessageRequest(MessageType.FILE, null, List.of(8L), "client-1")));

        assertEquals(403, exception.getCode());
        verify(communicationRepository, never()).saveMessage(any());
    }

    @Test
    void memberCannotMarkReadWithMessageFromAnotherConversation() {
        when(communicationRepository.findConversationByIdForUpdate(1L)).thenReturn(Optional.of(conversation()));
        when(communicationRepository.findMessageById(12L)).thenReturn(Optional.of(MessageEntity.builder()
                .id(12L)
                .conversationId(2L)
                .build()));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));

        ApiException exception = assertThrows(ApiException.class,
                () -> conversationService.markRead(1L, new ConversationModels.MarkReadRequest(12L)));

        assertEquals(403, exception.getCode());
        verify(communicationRepository, never()).resetReadState(any(), any(), any());
    }

    @Test
    void messageSenderCannotDeleteMessageOutsideOwnConversation() {
        when(communicationRepository.findMessageById(12L)).thenReturn(Optional.of(MessageEntity.builder()
                .id(12L)
                .conversationId(2L)
                .senderId(1L)
                .build()));
        when(communicationRepository.findConversationByIdForUpdate(2L)).thenReturn(Optional.of(ConversationEntity.builder()
                .id(2L)
                .customerId(3L)
                .designerId(4L)
                .build()));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));

        ApiException exception = assertThrows(ApiException.class,
                () -> conversationService.deleteMessage(12L));

        assertEquals(403, exception.getCode());
        verify(communicationRepository, never()).saveMessage(any());
    }

    @Test
    void participantCannotDeleteSystemMessage() {
        when(communicationRepository.findMessageById(12L)).thenReturn(Optional.of(MessageEntity.builder()
                .id(12L)
                .conversationId(1L)
                .senderId(null)
                .build()));
        when(communicationRepository.findConversationByIdForUpdate(1L)).thenReturn(Optional.of(conversation()));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));

        ApiException exception = assertThrows(ApiException.class,
                () -> conversationService.deleteMessage(12L));

        assertEquals(403, exception.getCode());
        verify(communicationRepository, never()).saveMessage(any());
    }

    @Test
    void deletingOwnMessageRefreshesBothParticipantsUnreadCounts() {
        MessageEntity message = MessageEntity.builder()
                .id(12L)
                .conversationId(1L)
                .senderId(1L)
                .build();
        when(communicationRepository.findMessageById(12L)).thenReturn(Optional.of(message));
        when(communicationRepository.findConversationByIdForUpdate(1L)).thenReturn(Optional.of(conversation()));
        when(authService.currentUserProfile()).thenReturn(user(1L, UserRole.CUSTOMER));
        when(communicationRepository.saveMessage(message)).thenReturn(message);
        when(communicationRepository.findLatestActiveMessage(1L)).thenReturn(Optional.empty());

        conversationService.deleteMessage(12L);

        verify(communicationRepository).saveConversation(conversation());
        verify(communicationRepository).refreshUnreadCount(1L, 1L);
        verify(communicationRepository).refreshUnreadCount(1L, 2L);
    }

    private ConversationEntity conversation() {
        return ConversationEntity.builder()
                .id(1L)
                .projectId(10L)
                .customerId(1L)
                .designerId(2L)
                .build();
    }

    private UserProfile user(Long id, UserRole role) {
        return new UserProfile(id, "用户" + id, role, null, UserStatus.ENABLED);
    }
}
