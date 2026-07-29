package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.identity.model.ActorRef;

import java.time.LocalDateTime;
import java.util.Set;

public interface ProjectAuthorizationService {
    AuthorizationDecision authorize(Long projectId, ProjectAction action);

    ProjectFileAccessDecision authorizeProjectFile(Long fileId);

    record ProjectFileAccessDecision(boolean projectFile, boolean allowed) {
    }

    enum ProjectAction {
        VIEW_SUMMARY,
        VIEW_FULL,
        VIEW_SENSITIVE,
        VIEW_COMMERCIAL,
        SEND_MESSAGE,
        UPDATE_PROJECT,
        MANAGE_PROJECT_FILE,
        CONFIRM_REQUIREMENT,
        CONFIRM_REPORT,
        CONFIRM_DESIGN,
        SIGN_CONTRACT,
        MANAGE_PAYMENT,
        RECEIVE_DELIVERY,
        TAKEOVER,
        REVIEW_ARTIFACT,
        ADJUST_ASSIGNMENT
    }

    enum AccessLevel {
        NONE,
        SUMMARY,
        FULL,
        SENSITIVE,
        COMMERCIAL,
        ADMIN
    }

    record AuthorizationDecision(boolean allowed, AccessLevel accessLevel, AuthorizationBasis basis) {
    }

    record AuthorizationBasis(String source,
                              Long projectId,
                              ProjectAction action,
                              ActorRef actor,
                              LocalDateTime decidedAt,
                              Long relationshipId,
                              Long relationshipVersion,
                              Long organizationMembershipId,
                              Long organizationMembershipVersion,
                              Set<String> scopes) {
        public AuthorizationBasis {
            scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        }

        public AuthorizationBasis(String source, Long relationshipId, Set<String> scopes) {
            this(source, null, null, null, LocalDateTime.now(), relationshipId, null, null, null, scopes);
        }
    }
}
