package com.advertisementdesign.back.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新用户资料请求")
public record UpdateUserRequest(String nickname, String avatar, String phone) {
}
