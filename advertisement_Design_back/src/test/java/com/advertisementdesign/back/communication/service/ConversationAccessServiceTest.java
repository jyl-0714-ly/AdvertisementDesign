package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationAccessServiceTest {
    @Mock private CommunicationRepository communicationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private AuthService authService;

    private ConversationAccessService service;

    @BeforeEach
    void setUp() {
        service = new ConversationAccessService(
                communicationRepository,
                projectRepository,
                authService);
    }

    @Test
    void projectUsesCurrentProjectParticipantsInsteadOfConversationColumns() {
        ConversationEntity conversation = ConversationEntity.builder()
                .id(100L)
                .projectId(99L)
                .customerId(11L)
                .designerId(22L)
                .build();
        when(projectRepository.findProjectById(99L)).thenReturn(Optional.of(
                ProjectEntity.builder()
                        .id(99L)
                        .customerId(44L)
                        .designerId(33L)
                        .build()));

        assertFalse(service.canAccess(conversation, user(22L, UserRole.DESIGNER, UserStatus.ENABLED)));
        assertFalse(service.canAccess(conversation, user(11L, UserRole.CUSTOMER, UserStatus.ENABLED)));
        assertTrue(service.canAccess(conversation, user(33L, UserRole.DESIGNER, UserStatus.ENABLED)));
        assertTrue(service.canAccess(conversation, user(44L, UserRole.CUSTOMER, UserStatus.ENABLED)));
    }

    @Test
    void disabledOrRoleMismatchedUserCannotAccessConversation() {
        ConversationEntity conversation = ConversationEntity.builder()
                .id(100L)
                .customerId(11L)
                .designerId(22L)
                .build();

        assertFalse(service.canAccess(conversation, user(11L, UserRole.CUSTOMER, UserStatus.DISABLED)));
        assertFalse(service.canAccess(conversation, user(11L, UserRole.DESIGNER, UserStatus.ENABLED)));
    }

    @Test
    void attachedFileUsesAuthoritativeConversationAccess() {
        ConversationEntity conversation = ConversationEntity.builder()
                .id(100L)
                .projectId(99L)
                .customerId(11L)
                .designerId(22L)
                .build();
        when(authService.currentUserProfile())
                .thenReturn(user(22L, UserRole.DESIGNER, UserStatus.ENABLED));
        when(communicationRepository.findConversationByAttachedFileId(8L))
                .thenReturn(Optional.of(conversation));
        when(projectRepository.findProjectById(99L)).thenReturn(Optional.of(
                ProjectEntity.builder()
                        .id(99L)
                        .customerId(11L)
                        .designerId(33L)
                        .build()));

        assertTrue(service.isAttachedToConversation(8L));
        assertFalse(service.canCurrentUserAccessAttachedFile(8L));
    }

    private UserProfile user(Long id, UserRole role, UserStatus status) {
        return new UserProfile(id, "用户" + id, role, null, status);
    }
}
