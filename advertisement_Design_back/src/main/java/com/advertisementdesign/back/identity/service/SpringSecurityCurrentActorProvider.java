package com.advertisementdesign.back.identity.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.model.ActorRef;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SpringSecurityCurrentActorProvider implements CurrentActorProvider {
    private final IdentityService identityService;

    public SpringSecurityCurrentActorProvider(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public CurrentActor requireCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser principal)) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED);
        }
        IdentityService.UserProfile user = identityService.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED));
        if (user.status() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED);
        }
        ActorRef.ActorType actorType = switch (user.role()) {
            case CUSTOMER -> ActorRef.ActorType.CUSTOMER_USER;
            case DESIGNER -> ActorRef.ActorType.DESIGNER_USER;
            case ADMIN -> ActorRef.ActorType.ADMIN_USER;
        };
        return new CurrentActor(new ActorRef(actorType, user.id()), user.nickname());
    }
}
