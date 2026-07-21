package com.advertisementdesign.back.api.auth;

import com.advertisementdesign.back.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "认证相关模型")
public final class AuthModels {
    private AuthModels() {
    }

    @Schema(description = "邮箱密码登录请求")
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    @Schema(description = "客户注册请求")
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字") String code,
            @NotBlank String password,
            @NotBlank String nickname) {
    }

    public enum EmailCodePurpose {
        REGISTER,
        LOGIN,
        RESET_PASSWORD
    }

    @Schema(description = "发送邮箱验证码请求")
    public record SendEmailCodeRequest(@NotBlank @Email String email, @NotNull EmailCodePurpose purpose) {
    }

    @Schema(description = "发送邮箱验证码响应")
    public record SendEmailCodeResponse(long expiresInSeconds) {
    }

    @Schema(description = "邮箱验证码登录请求")
    public record EmailCodeLoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字") String code) {
    }

    @Schema(description = "重置密码请求")
    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字") String code,
            @NotBlank String password) {
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
