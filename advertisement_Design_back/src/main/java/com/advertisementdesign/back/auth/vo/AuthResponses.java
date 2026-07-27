package com.advertisementdesign.back.auth.vo;

import com.advertisementdesign.back.identity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AuthResponses {
    private AuthResponses() {
    }

    @Schema(description = "登录响应")
    public record LoginResponse(@NotBlank String token, @NotNull UserVO user) {}

    @Schema(description = "发送邮箱验证码响应")
    public record SendEmailCodeResponse(long expiresInSeconds) {}

    @Schema(description = "用户视图")
    public record UserVO(Long id, String email, String nickname, UserRole role, String avatar) {}
}
