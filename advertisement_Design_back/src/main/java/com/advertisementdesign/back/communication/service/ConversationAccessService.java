package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConversationAccessService {
    private final CommunicationRepository communicationRepository;
    private final ProjectRepository projectRepository;
    private final AuthService authService;

    public void validateCurrentUser(ConversationEntity conversation) {
        if (!canAccess(conversation, authService.currentUserProfile())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    public boolean isAttachedToConversation(Long fileId) {
        return communicationRepository.findConversationByAttachedFileId(fileId).isPresent();
    }

    public boolean canCurrentUserAccessAttachedFile(Long fileId) {
        UserProfile currentUser = authService.currentUserProfile();
        if (currentUser.status() != UserStatus.ENABLED) {
            return false;
        }
        return communicationRepository.findConversationByAttachedFileId(fileId)
                .map(conversation -> canAccess(conversation, currentUser))
                .orElse(false);
    }

    public boolean canAccess(ConversationEntity conversation, UserProfile user) {
        if (user.status() != UserStatus.ENABLED) {
            return false;
        }
        AuthoritativeParticipants participants = resolveParticipants(conversation);
        return participants != null && isAuthoritativeParticipant(
                user, participants.customerId(), participants.designerId());
    }

    public AuthoritativeParticipants requireParticipants(ConversationEntity conversation) {
        AuthoritativeParticipants participants = resolveParticipants(conversation);
        if (participants == null) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return participants;
    }

    private AuthoritativeParticipants resolveParticipants(ConversationEntity conversation) {
        if (conversation.getProjectId() != null) {
            ProjectEntity project = projectRepository.findProjectById(conversation.getProjectId())
                    .orElse(null);
            return project == null ? null : new AuthoritativeParticipants(
                    project.getCustomerId(), project.getDesignerId());
        }
        return null;
    }

    public record AuthoritativeParticipants(Long customerId, Long designerId) {
    }

    private boolean isAuthoritativeParticipant(
            UserProfile user,
            Long customerId,
            Long designerId) {
        return user.role() == UserRole.CUSTOMER && Objects.equals(customerId, user.id())
                || user.role() == UserRole.DESIGNER && Objects.equals(designerId, user.id());
    }
}
