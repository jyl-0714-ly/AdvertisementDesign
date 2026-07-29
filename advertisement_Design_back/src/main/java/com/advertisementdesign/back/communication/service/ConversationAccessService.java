package com.advertisementdesign.back.communication.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationAccessService {
    private final CommunicationRepository communicationRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public void validateCurrentUser(ConversationEntity conversation) {
        if (!authorize(conversation).allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    public boolean isAttachedToConversation(Long fileId) {
        return communicationRepository.findConversationByAttachedFileId(fileId).isPresent();
    }

    public boolean canCurrentUserAccessAttachedFile(Long fileId) {
        return communicationRepository.findConversationByAttachedFileId(fileId)
                .map(conversation -> authorize(conversation).allowed())
                .orElse(false);
    }

    public ProjectAuthorizationService.AuthorizationDecision authorize(ConversationEntity conversation) {
        if (conversation.getProjectId() == null) {
            return new ProjectAuthorizationService.AuthorizationDecision(
                    false,
                    ProjectAuthorizationService.AccessLevel.NONE,
                    new ProjectAuthorizationService.AuthorizationBasis(
                            "CONVERSATION_WITHOUT_PROJECT", null,
                            ProjectAuthorizationService.ProjectAction.VIEW_FULL, null,
                            java.time.LocalDateTime.now(), null, null, null, null, java.util.Set.of()));
        }
        return projectAuthorizationService.authorize(
                conversation.getProjectId(), ProjectAuthorizationService.ProjectAction.VIEW_FULL);
    }
}
