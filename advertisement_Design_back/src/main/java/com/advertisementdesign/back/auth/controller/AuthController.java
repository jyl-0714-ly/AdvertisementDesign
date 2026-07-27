package com.advertisementdesign.back.auth.controller;

import com.advertisementdesign.back.auth.dto.AuthRequests;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.auth.vo.AuthResponses;
import com.advertisementdesign.back.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "邮箱密码登录")
    @PostMapping("/login")
    public Result<AuthResponses.LoginResponse> login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email-codes")
    public Result<AuthResponses.SendEmailCodeResponse> sendEmailCode(@Valid @RequestBody AuthRequests.SendEmailCodeRequest request) {
        return Result.success(authService.sendEmailCode(request));
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login-by-email-code")
    public Result<AuthResponses.LoginResponse> loginByEmailCode(@Valid @RequestBody AuthRequests.EmailCodeLoginRequest request) {
        return Result.success(authService.loginByEmailCode(request));
    }

    @Operation(summary = "重置客户密码")
    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@Valid @RequestBody AuthRequests.ResetPasswordRequest request) {
        return Result.success(authService.resetPassword(request));
    }

    @Operation(summary = "客户注册")
    @PostMapping("/register")
    public Result<AuthResponses.UserVO> register(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<AuthResponses.UserVO> me() {
        return Result.success(authService.me());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        return Result.success(authService.logout());
    }
}
