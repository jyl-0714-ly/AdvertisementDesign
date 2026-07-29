package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.communication.converter.ConversationConverter;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.enums.ConversationStatus;
import com.advertisementdesign.back.communication.enums.MessageSendSource;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    @Mock private CommunicationRepository repository;
    @Mock private ProjectAuthorizationService authorizationService;
    @Mock private ProjectQueryService projectQueryService;
    @Mock private CurrentActorProvider currentActorProvider;
    @Mock private CurrentUserProfileProvider currentUserProfileProvider;

    @Test
    void currentCustomerAppendDerivesActorSourceIdentityAndAuthorizationEvidence() throws Exception {
        ActorRef actor = new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 7L);
        var basis = new ProjectAuthorizationService.AuthorizationBasis(
                "CUSTOMER_PROJECT_MEMBER", 101L, ProjectAuthorizationService.ProjectAction.SEND_MESSAGE,
                actor, LocalDateTime.of(2026, 7, 29, 11, 0), 55L, 2L, 66L, 3L, Set.of("PRIMARY_CONTACT"));
        when(currentActorProvider.requireCurrentActor())
                .thenReturn(new CurrentActorProvider.CurrentActor(actor, "untrusted principal label"));
        when(currentUserProfileProvider.currentUserProfile()).thenReturn(
                new IdentityService.UserProfile(7L, "可信客户名", UserRole.CUSTOMER, null, UserStatus.ENABLED));
        when(authorizationService.authorize(101L, ProjectAuthorizationService.ProjectAction.SEND_MESSAGE))
                .thenReturn(new ProjectAuthorizationService.AuthorizationDecision(
                        true, ProjectAuthorizationService.AccessLevel.FULL, basis));
        when(projectQueryService.findContext(101L)).thenReturn(Optional.of(
                new ProjectModels.ProjectContextView(101L, 20L, ProjectStatus.ACTIVE, 1L)));
        when(repository.findConversationByProjectId(101L)).thenReturn(Optional.of(
                ConversationEntity.builder().id(88L).projectId(101L).status(ConversationStatus.ACTIVE).version(0L).build()));
        doAnswer(invocation -> {
            invocation.<MessageEntity>getArgument(0).setId(99L);
            return invocation.getArgument(0);
        }).when(repository).appendMessage(any(MessageEntity.class), any());
        when(repository.listAttachments(99L)).thenReturn(List.of());
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ConversationService service = new ConversationService(
                repository, new ConversationConverter(), authorizationService, projectQueryService,
                currentActorProvider, currentUserProfileProvider, objectMapper);

        service.appendAsCurrentUser(new ConversationModels.CurrentUserAppendCommand(
                101L, MessageType.TEXT, "新需求", null, null, "client-1", List.of()));

        ArgumentCaptor<MessageEntity> messageCaptor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(repository).appendMessage(messageCaptor.capture(), any());
        MessageEntity message = messageCaptor.getValue();
        assertEquals(ActorRef.ActorType.CUSTOMER_USER, message.getActorType());
        assertEquals(7L, message.getActorId());
        assertEquals(MessageSendSource.CUSTOMER_UI, message.getSendSource());
        assertEquals("可信客户名", message.getCustomerDisplayIdentity());
        var serializedBasis = objectMapper.readTree(message.getAuthorizationBasis());
        assertEquals("CUSTOMER_PROJECT_MEMBER", serializedBasis.get("source").asText());
        assertEquals("SEND_MESSAGE", serializedBasis.get("action").asText());
        assertFalse(message.getAuthorizationBasis().contains("untrusted principal label"));
    }

    @Test
    void trustedAppendRejectsAuthorizationEvidenceForAnotherActor() {
        ActorRef actor = new ActorRef(ActorRef.ActorType.DESIGNER_USER, 7L);
        ActorRef fabricatedBasisActor = new ActorRef(ActorRef.ActorType.DESIGNER_USER, 8L);
        var basis = new ProjectAuthorizationService.AuthorizationBasis(
                "PROJECT_ASSIGNMENT", 101L, ProjectAuthorizationService.ProjectAction.SEND_MESSAGE,
                fabricatedBasisActor, LocalDateTime.of(2026, 7, 29, 12, 0), 55L, 2L, null, null, Set.of("FULL"));
        when(projectQueryService.findContext(101L)).thenReturn(Optional.of(
                new ProjectModels.ProjectContextView(101L, 20L, ProjectStatus.ACTIVE, 1L)));
        when(repository.findConversationByProjectId(101L)).thenReturn(Optional.of(
                ConversationEntity.builder().id(88L).projectId(101L).status(ConversationStatus.ACTIVE).version(0L).build()));
        ConversationService service = new ConversationService(
                repository, new ConversationConverter(), authorizationService, projectQueryService,
                currentActorProvider, currentUserProfileProvider, new ObjectMapper().findAndRegisterModules());

        ApiException exception = assertThrows(ApiException.class, () -> service.appendTrustedInternal(
                new ConversationModels.TrustedInternalAppendCommand(
                        101L, MessageType.TEXT, "内部消息", "伪造身份", actor, MessageSendSource.DESIGNER_UI,
                        basis, null, null, "internal-1", List.of(), null)));

        assertEquals(403, exception.getCode());
    }
}
