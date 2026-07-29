package com.advertisementdesign.back.identity.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserProfileProvider {
    private final CurrentActorProvider currentActorProvider;
    private final IdentityService identityService;

    public CurrentUserProfileProvider(CurrentActorProvider currentActorProvider, IdentityService identityService) {
        this.currentActorProvider = currentActorProvider;
        this.identityService = identityService;
    }

    public IdentityService.UserProfile currentUserProfile() {
        return identityService.findById(currentActorProvider.requireCurrentActor().actor().actorId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
    }
}
