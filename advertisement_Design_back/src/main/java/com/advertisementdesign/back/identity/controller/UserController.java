package com.advertisementdesign.back.identity.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.converter.UserConverter;
import com.advertisementdesign.back.identity.dto.UpdateUserRequest;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.OrganizationMembershipService;
import com.advertisementdesign.back.identity.service.IdentityService.UserAccount;
import com.advertisementdesign.back.identity.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User", description = "用户接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final IdentityService identityService;
    private final CurrentActorProvider currentActorProvider;
    private final OrganizationMembershipService organizationMembershipService;

    @Operation(summary = "当前客户的有效组织")
    @GetMapping("/me/organizations")
    public Result<List<OrganizationMembershipService.OrganizationContext>> myOrganizations() {
        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        if (actor.type() != ActorRef.ActorType.CUSTOMER_USER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "仅客户账号可以读取客户组织");
        }
        return Result.success(organizationMembershipService.listActiveOrganizations(actor.actorId()));
    }

    @Operation(summary = "更新当前用户资料")
    @PutMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        UserAccount user = identityService.updateProfile(currentActorProvider.requireCurrentActor().actor().actorId(), request.nickname(), request.avatarFileId(), request.phone());
        return Result.success(UserConverter.toVO(user));
    }
}
