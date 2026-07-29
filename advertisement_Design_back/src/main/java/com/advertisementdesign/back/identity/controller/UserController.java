package com.advertisementdesign.back.identity.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.converter.UserConverter;
import com.advertisementdesign.back.identity.dto.UpdateUserRequest;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserAccount;
import com.advertisementdesign.back.identity.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "用户接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final IdentityService identityService;
    private final CurrentActorProvider currentActorProvider;

    @Operation(summary = "更新当前用户资料")
    @PutMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        UserAccount user = identityService.updateProfile(currentActorProvider.requireCurrentActor().actor().actorId(), request.nickname(), request.avatarFileId(), request.phone());
        return Result.success(UserConverter.toVO(user));
    }
}
