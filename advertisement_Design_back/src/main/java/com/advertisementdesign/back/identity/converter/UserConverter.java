package com.advertisementdesign.back.identity.converter;

import com.advertisementdesign.back.identity.service.IdentityService.UserAccount;
import com.advertisementdesign.back.identity.vo.UserVO;

public final class UserConverter {
    private UserConverter() {
    }

    public static UserVO toVO(UserAccount user) {
        return new UserVO(user.id(), user.email(), user.nickname(), user.role(), user.avatar());
    }
}
