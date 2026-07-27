package com.advertisementdesign.back.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "认证请求模型")
public final class AuthRequests {
    private AuthRequests() {
    }

    @Schema(description = "邮箱密码登录请求")
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    @Schema(description = "客户注册请求")
    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字") String code,
                                  @NotBlank String password, @NotBlank String nickname) {}

    @Schema(description = "发送邮箱验证码请求")
    public record SendEmailCodeRequest(@NotBlank @Email String email, @NotNull EmailCodePurpose purpose) {}

    @Schema(description = "邮箱验证码登录请求")
    public record EmailCodeLoginRequest(@NotBlank @Email String email,
                                        @NotBlank @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字") String code) {}

    @Schema(description = "重置密码请求")
    public record ResetPasswordRequest(@NotBlank @Email String email,
                                       @NotBlank @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字") String code,
                                       @NotBlank String password) {}

    public enum EmailCodePurpose { REGISTER, LOGIN, RESET_PASSWORD }
}
