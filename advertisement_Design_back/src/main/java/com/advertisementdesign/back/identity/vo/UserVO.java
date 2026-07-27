package com.advertisementdesign.back.identity.vo;

import com.advertisementdesign.back.identity.enums.UserRole;

public record UserVO(Long id, String email, String nickname, UserRole role, String avatar) {
}
