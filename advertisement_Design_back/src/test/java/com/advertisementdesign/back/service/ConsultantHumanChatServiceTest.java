package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.consultant.ConsultantHumanChatModels;
import com.advertisementdesign.back.api.consultant.ConsultantIntakeModels;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.domain.enums.UserStatus;
import com.advertisementdesign.back.store.DemoDataStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultantHumanChatServiceTest {

    @Test
    void customerAndMatchedDesignerCanUsePersistedChatWithAuthenticatedSender() {
        DemoDataStore store = new DemoDataStore();
        AuthService intakeAuth = authFor(store, 1L);
        ConsultantIntakeModels.ConsultantIntakeVO intake = new ConsultantIntakeService(store, intakeAuth)
                .submit(intakeRequest());

        ConsultantHumanChatService customerService = new ConsultantHumanChatService(store, authFor(store, 1L));
        ConsultantHumanChatModels.HumanMessageVO sent = customerService.sendMessage(
                intake.humanChatId(), new ConsultantHumanChatModels.SendHumanMessageRequest("  请优先确认品牌色  "));

        assertEquals(1L, sent.senderId());
        assertEquals(MessageSenderRole.CUSTOMER, sent.senderRole());
        assertEquals("请优先确认品牌色", sent.content());
        List<ConsultantHumanChatModels.HumanMessageVO> messages = customerService.listMessages(intake.humanChatId());
        assertEquals(3, messages.size());
        assertEquals(2, messages.stream().filter(message -> message.senderRole() == MessageSenderRole.DESIGNER).count());

        ConsultantHumanChatService designerService = new ConsultantHumanChatService(store, authFor(store, 2L));
        ConsultantHumanChatModels.HumanMessageVO reply = designerService.sendMessage(
                intake.humanChatId(), new ConsultantHumanChatModels.SendHumanMessageRequest("收到"));
        assertEquals(2L, reply.senderId());
        assertEquals(MessageSenderRole.DESIGNER, reply.senderRole());
    }

    @Test
    void unrelatedCustomerCannotAccessChat() {
        DemoDataStore store = new DemoDataStore();
        ConsultantIntakeModels.ConsultantIntakeVO intake = new ConsultantIntakeService(store, authFor(store, 1L))
                .submit(intakeRequest());
        UserEntity anotherCustomer = UserEntity.builder()
                .id(3L)
                .email("another@163.com")
                .passwordHash("not-used")
                .nickname("其他客户")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ENABLED)
                .build();
        store.saveUser(anotherCustomer);
        ConsultantHumanChatService service = new ConsultantHumanChatService(store, authFor(store, 3L));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.listMessages(intake.humanChatId()));

        assertEquals(403, exception.getCode());
    }

    @Test
    void disabledMatchedDesignerCannotAccessChat() {
        DemoDataStore store = new DemoDataStore();
        ConsultantIntakeModels.ConsultantIntakeVO intake = new ConsultantIntakeService(store, authFor(store, 1L))
                .submit(intakeRequest());
        UserEntity designer = store.findUserById(2L).orElseThrow();
        designer.setStatus(UserStatus.DISABLED);
        ConsultantHumanChatService service = new ConsultantHumanChatService(store, authFor(store, 2L));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.listMessages(intake.humanChatId()));

        assertEquals(403, exception.getCode());
    }

    private AuthService authFor(DemoDataStore store, Long userId) {
        AuthService authService = mock(AuthService.class);
        when(authService.currentUserEntity()).thenReturn(store.findUserById(userId).orElseThrow());
        return authService;
    }

    private ConsultantIntakeModels.SubmitConsultantIntakeRequest intakeRequest() {
        return new ConsultantIntakeModels.SubmitConsultantIntakeRequest(
                "品牌设计", "餐饮", "需要完成品牌视觉升级", "1-2 万元", "4 周");
    }
}
