package com.advertisementdesign.back.controller;

import com.advertisementdesign.back.api.auth.AuthModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "用户接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @Operation(summary = "更新当前用户资料")
    @PutMapping("/me")
    public Result<AuthModels.UserVO> updateMe(@Valid @org.springframework.web.bind.annotation.RequestBody AuthModels.UpdateUserRequest request) {
        return Result.success(authService.updateMe(request));
    }
}
