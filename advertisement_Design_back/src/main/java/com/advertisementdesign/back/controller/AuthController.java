package com.advertisementdesign.back.controller;

import com.advertisementdesign.back.api.auth.AuthModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public Result<AuthModels.LoginResponse> login(@Valid @org.springframework.web.bind.annotation.RequestBody AuthModels.LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "客户注册")
    @PostMapping("/register")
    public Result<AuthModels.UserVO> register(@Valid @org.springframework.web.bind.annotation.RequestBody AuthModels.RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<AuthModels.UserVO> me() {
        return Result.success(authService.me());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        return Result.success(authService.logout());
    }
}
