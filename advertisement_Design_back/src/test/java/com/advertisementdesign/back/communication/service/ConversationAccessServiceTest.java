package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationAccessServiceTest {
    @Mock private CommunicationRepository communicationRepository;
    @Mock private ProjectAuthorizationService projectAuthorizationService;

    private ConversationAccessService service;

    @BeforeEach
    void setUp() {
        service = new ConversationAccessService(communicationRepository, projectAuthorizationService);
    }

    @Test
    void projectAuthorizationDecisionControlsConversationAccess() {
        ConversationEntity conversation = ConversationEntity.builder().id(100L).projectId(99L).build();
        when(projectAuthorizationService.authorize(99L, ProjectAuthorizationService.ProjectAction.VIEW_FULL))
                .thenReturn(decision(true));

        assertTrue(service.authorize(conversation).allowed());
    }

    @Test
    void conversationWithoutProjectIsDenied() {
        ConversationEntity conversation = ConversationEntity.builder().id(100L).build();

        assertFalse(service.authorize(conversation).allowed());
    }

    @Test
    void attachedFileUsesProjectAuthorizationDecision() {
        ConversationEntity conversation = ConversationEntity.builder().id(100L).projectId(99L).build();
        when(communicationRepository.findConversationByAttachedFileId(8L))
                .thenReturn(Optional.of(conversation));
        when(projectAuthorizationService.authorize(99L, ProjectAuthorizationService.ProjectAction.VIEW_FULL))
                .thenReturn(decision(false));

        assertTrue(service.isAttachedToConversation(8L));
        assertFalse(service.canCurrentUserAccessAttachedFile(8L));
    }

    private ProjectAuthorizationService.AuthorizationDecision decision(boolean allowed) {
        return new ProjectAuthorizationService.AuthorizationDecision(
                allowed,
                allowed ? ProjectAuthorizationService.AccessLevel.FULL : ProjectAuthorizationService.AccessLevel.NONE,
                new ProjectAuthorizationService.AuthorizationBasis("TEST", 1L, Set.of()));
    }
}
