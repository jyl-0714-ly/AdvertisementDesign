package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.consultation.entity.ConsultantHumanMessageEntity;
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
import static org.mockito.Mockito.when;

class ConsultantHumanChatServiceTest {

    @Test
    void customerAndMatchedDesignerCanUsePersistedChatWithAuthenticatedSender() {
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

        ConsultantHumanChatService designerService = fixture.serviceFor(fixture.designer);
        ConsultantHumanChatModels.HumanMessageVO reply = designerService.sendMessage(
                fixture.intake.getHumanChatId(),
                new ConsultantHumanChatModels.SendHumanMessageRequest("收到"));
        assertEquals(2L, reply.senderId());
        assertEquals(MessageSenderRole.DESIGNER, reply.senderRole());
    }

    @Test
    void unrelatedCustomerCannotAccessChat() {
        Fixture fixture = new Fixture();
        UserEntity anotherCustomer = fixture.addUser(3L, "其他客户", UserRole.CUSTOMER);
        ConsultantHumanChatService service = fixture.serviceFor(anotherCustomer);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.listMessages(fixture.intake.getHumanChatId()));

        assertEquals(403, exception.getCode());
    }

    @Test
    void disabledMatchedDesignerCannotAccessChat() {
        Fixture fixture = new Fixture();
        fixture.designer.setStatus(UserStatus.DISABLED);
        ConsultantHumanChatService service = fixture.serviceFor(fixture.designer);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.listMessages(fixture.intake.getHumanChatId()));

        assertEquals(403, exception.getCode());
    }

    private static final class Fixture {
        private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        private final IdentityService identityService = mock(IdentityService.class);
        private final Map<Long, UserEntity> users = new HashMap<>();
        private final List<ConsultantHumanMessageEntity> messages = new ArrayList<>();
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
            messages.add(message(1L, designer, "您好，我是演示设计师。"));
            messages.add(message(2L, designer, "您的需求已整理完成，请稍等。"));

            when(identityService.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(users.get(invocation.getArgument(0, Long.class)))
                            .map(this::toProfile));
            when(consultationRepository.findIntakeByHumanChatId(any())).thenAnswer(invocation -> {
                String humanChatId = invocation.getArgument(0, String.class);
                return intake.getHumanChatId().equals(humanChatId)
                        ? Optional.of(intake)
                        : Optional.empty();
            });
            when(consultationRepository.listHumanMessages(any())).thenAnswer(invocation -> {
                String humanChatId = invocation.getArgument(0, String.class);
                return messages.stream()
                        .filter(message -> humanChatId.equals(message.getHumanChatId()))
                        .toList();
            });
            when(consultationRepository.saveHumanMessage(any())).thenAnswer(invocation -> {
                ConsultantHumanMessageEntity message = invocation.getArgument(0);
                message.setId(messageSequence.incrementAndGet());
                message.setCreatedAt(LocalDateTime.now());
                messages.add(message);
                return message;
            });
        }

        private ConsultantHumanChatService serviceFor(UserEntity currentUser) {
            AuthService authService = mock(AuthService.class);
            when(authService.currentUserProfile()).thenReturn(toProfile(currentUser));
            return new ConsultantHumanChatService(
                    consultationRepository,
                    identityService,
                    authService
            );
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

        private ConsultantHumanMessageEntity message(
                Long id,
                UserEntity sender,
                String content) {
            return ConsultantHumanMessageEntity.builder()
                    .id(id)
                    .humanChatId(intake.getHumanChatId())
                    .senderId(sender.getId())
                    .senderRole(MessageSenderRole.DESIGNER)
                    .content(content)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }
}
