package com.advertisementdesign.back.project.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.project.dto.FirstRequirementRequest;
import com.advertisementdesign.back.project.dto.ProjectNamingRequests;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.FirstRequirementProjectCreationService;
import com.advertisementdesign.back.project.service.ProjectNamingService;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import com.advertisementdesign.back.project.vo.FirstRequirementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Project", description = "项目查询接口")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Validated
public class ProjectController {
    private final ProjectQueryService projectQueryService;
    private final ProjectNamingService projectNamingService;
    private final FirstRequirementProjectCreationService firstRequirementProjectCreationService;

    @Operation(
            summary = "提交首条有效需求并原子创建项目",
            description = "无效需求仅返回引导；有效需求在一个事务中创建项目基础记录。相同幂等键重放首次成功结果。")
    @PostMapping("/from-first-requirement")
    public Result<FirstRequirementResponse> createFromFirstRequirement(
            @Parameter(required = true, description = "客户端生成的建项幂等键",
                    schema = @Schema(minLength = 1, maxLength = 128))
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody FirstRequirementRequest request) {
        return Result.success(firstRequirementProjectCreationService.create(request, idempotencyKey.strip()));
    }

    @Operation(summary = "当前用户可见项目摘要")
    @GetMapping
    public Result<List<ProjectModels.ProjectSummaryView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(projectQueryService.listAuthorizedSummaries(status, keyword));
    }

    @Operation(summary = "项目完整详情")
    @GetMapping("/{projectId}")
    public Result<ProjectModels.ProjectFullDetailView> detail(@PathVariable Long projectId) {
        return Result.success(projectQueryService.requireFullDetail(projectId));
    }

    @Operation(summary = "手动修改项目名称")
    @PostMapping("/{projectId}/name/manual")
    public Result<ProjectModels.ProjectFullDetailView> renameManually(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectNamingRequests.ManualRename request) {
        return Result.success(projectNamingService.renameManually(projectId, request.name(), request.version()));
    }

    @Operation(summary = "恢复项目自动命名")
    @PostMapping("/{projectId}/name/restore-auto")
    public Result<ProjectModels.ProjectFullDetailView> restoreAutomaticNaming(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectNamingRequests.RestoreAutomatic request) {
        return Result.success(projectNamingService.restoreAutomatic(projectId, request.version()));
    }

    @Operation(summary = "项目有效客户成员")
    @GetMapping("/{projectId}/customer-members")
    public Result<List<ProjectModels.CustomerMemberView>> customerMembers(@PathVariable Long projectId) {
        return Result.success(projectQueryService.listActiveCustomerMembers(projectId));
    }

    @Operation(summary = "项目当前主负责设计师分配")
    @GetMapping("/{projectId}/assignments/current-responsible")
    public Result<ProjectModels.AssignmentView> currentResponsible(@PathVariable Long projectId) {
        return Result.success(projectQueryService.currentResponsibleDesigner(projectId).orElse(null));
    }

    @Operation(summary = "项目设计师分配历史")
    @GetMapping("/{projectId}/assignments")
    public Result<List<ProjectModels.AssignmentView>> assignmentHistory(@PathVariable Long projectId) {
        return Result.success(projectQueryService.assignmentHistory(projectId));
    }
}
