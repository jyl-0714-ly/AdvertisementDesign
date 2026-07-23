package com.advertisementdesign.back.common.web;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {
    private AuthContext() {
    }

    public static CurrentUser currentUser() {
        CurrentUser currentUser = currentUserOrNull();
        if (currentUser == null) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    public static CurrentUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            return null;
        }
        return currentUser;
    }
}
