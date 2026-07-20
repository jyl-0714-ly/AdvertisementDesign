package com.advertisementdesign.back.api.auth;

import com.advertisementdesign.back.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "认证相关模型")
public final class AuthModels {
    private AuthModels() {
    }

    @Schema(description = "邮箱密码登录请求")
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    @Schema(description = "客户注册请求")
    public record RegisterRequest(@NotBlank String email, @NotBlank String password, @NotBlank String nickname) {
    }

    @Schema(description = "更新用户资料请求")
    public record UpdateUserRequest(String nickname, String avatar, String phone) {
    }

    @Schema(description = "登录响应")
    public record LoginResponse(@NotBlank String token, @NotNull UserVO user) {
    }

    @Schema(description = "用户视图")
    public record UserVO(Long id, String email, String nickname, UserRole role, String avatar) {
    }
}
