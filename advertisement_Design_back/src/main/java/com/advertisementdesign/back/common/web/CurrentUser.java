package com.advertisementdesign.back.common.web;

import com.advertisementdesign.back.identity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {
    private Long id;
    private String email;
    private String nickname;
    private UserRole role;
}
