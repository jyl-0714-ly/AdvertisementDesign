package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.communication.service.UnifiedConversationService;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.model.ConsultantHumanChatModels;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.entity.UserEntity;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultantHumanChatServiceTest {

    @Test
    void customerAndMatchedDesignerUseUnifiedMessagesWithAuthenticatedSender() {
        Fixture fixture = new Fixture();
        ConsultantHumanChatService customerService = fixture.serviceFor(fixture.customer);

        ConsultantHumanChatModels.HumanMessageVO sent = customerService.sendMessage(
                fixture.intake.getHumanChatId(),
                new ConsultantHumanChatModels.SendHumanMessageRequest("  请优先确认品牌色  "));

        assertEquals(1L, sent.senderId());
        assertEquals(MessageSenderRole.CUSTOMER, sent.senderRole());
        assertEquals("请优先确认品牌色", sent.content());
        List<ConsultantHumanChatModels.HumanMessageVO> messages = customerService
                .listMessages(fixture.intake.getHumanChatId());
        assertEquals(3, messages.size());
        assertEquals(2, messages.stream()
                .filter(message -> message.senderRole() == MessageSenderRole.DESIGNER)
                .count());
        verify(fixture.acknowledgementService, never())
                .acknowledgeHumanDesignerMessage(anyLong(), anyLong());

        ConsultantHumanChatModels.HumanMessageVO reply = fixture.serviceFor(fixture.designer).sendMessage(
                fixture.intake.getHumanChatId(),
                new ConsultantHumanChatModels.SendHumanMessageRequest("收到"));
        assertEquals(2L, reply.senderId());
        assertEquals(MessageSenderRole.DESIGNER, reply.senderRole());
        verify(fixture.acknowledgementService)
                .acknowledgeHumanDesignerMessage(1L, 2L);
        verify(fixture.consultationRepository, never()).saveHumanMessage(any());
    }

    @Test
    void unrelatedCustomerCannotAccessChat() {
        Fixture fixture = new Fixture();
        UserEntity anotherCustomer = fixture.addUser(3L, "其他客户", UserRole.CUSTOMER);

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.serviceFor(anotherCustomer)
                        .listMessages(fixture.intake.getHumanChatId()));

        assertEquals(403, exception.getCode());
    }

    @Test
    void disabledMatchedDesignerCannotAccessChat() {
        Fixture fixture = new Fixture();
        fixture.designer.setStatus(UserStatus.DISABLED);

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.serviceFor(fixture.designer)
                        .listMessages(fixture.intake.getHumanChatId()));

        assertEquals(403, exception.getCode());
    }

    private static final class Fixture {
        private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        private final IdentityService identityService = mock(IdentityService.class);
        private final UnifiedConversationService unifiedConversationService = mock(UnifiedConversationService.class);
        private final ConsultationAcknowledgementService acknowledgementService =
                mock(ConsultationAcknowledgementService.class);
        private final Map<Long, UserEntity> users = new HashMap<>();
        private final List<MessageEntity> messages = new ArrayList<>();
        private final AtomicLong messageSequence = new AtomicLong(2);
        private final UserEntity customer;
        private final UserEntity designer;
        private final ConsultantIntakeEntity intake;

        private Fixture() {
            customer = addUser(1L, "演示客户", UserRole.CUSTOMER);
            designer = addUser(2L, "演示设计师", UserRole.DESIGNER);
            intake = ConsultantIntakeEntity.builder()
                    .id(1L)
                    .customerId(customer.getId())
                    .matchedDesignerId(designer.getId())
                    .humanChatId("consultant-test")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            messages.add(message(1L, designer, MessageSenderRole.DESIGNER, "您好，我是演示设计师。"));
            messages.add(message(2L, designer, MessageSenderRole.DESIGNER, "您的需求已整理完成，请稍等。"));

            when(identityService.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(users.get(invocation.getArgument(0, Long.class)))
                            .map(this::toProfile));
            when(consultationRepository.findIntakeByHumanChatId(any())).thenAnswer(invocation ->
                    intake.getHumanChatId().equals(invocation.getArgument(0, String.class))
                            ? Optional.of(intake) : Optional.empty());
            when(unifiedConversationService.listMessagesByConsultantIntakeId(1L))
                    .thenAnswer(invocation -> List.copyOf(messages));
            when(unifiedConversationService.appendHumanMessage(anyLong(), anyLong(), any(), any()))
                    .thenAnswer(invocation -> {
                        Long senderId = invocation.getArgument(1, Long.class);
                        MessageSenderRole role = invocation.getArgument(2, MessageSenderRole.class);
                        String content = invocation.getArgument(3, String.class).trim();
                        MessageEntity message = message(
                                messageSequence.incrementAndGet(), users.get(senderId), role, content);
                        messages.add(message);
                        return message;
                    });
        }

        private ConsultantHumanChatService serviceFor(UserEntity currentUser) {
            AuthService authService = mock(AuthService.class);
            when(authService.currentUserProfile()).thenReturn(toProfile(currentUser));
            return new ConsultantHumanChatService(
                    consultationRepository, identityService, authService,
                    unifiedConversationService, acknowledgementService);
        }

        private UserProfile toProfile(UserEntity user) {
            return new UserProfile(
                    user.getId(), user.getNickname(), user.getRole(), user.getAvatar(), user.getStatus());
        }

        private UserEntity addUser(Long id, String nickname, UserRole role) {
            UserEntity user = UserEntity.builder()
                    .id(id)
                    .email("user" + id + "@example.com")
                    .passwordHash("not-used")
                    .nickname(nickname)
                    .role(role)
                    .status(UserStatus.ENABLED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            users.put(id, user);
            return user;
        }

        private MessageEntity message(
                Long id, UserEntity sender, MessageSenderRole role, String content) {
            return MessageEntity.builder()
                    .id(id)
                    .conversationId(100L)
                    .senderId(sender.getId())
                    .senderRole(role)
                    .messageType(MessageType.TEXT)
                    .content(content)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .fileIds(List.of())
                    .build();
        }
    }
}
