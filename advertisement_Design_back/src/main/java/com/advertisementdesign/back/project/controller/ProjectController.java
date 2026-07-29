package com.advertisementdesign.back.project.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.service.ProjectQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Project", description = "项目查询接口")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectQueryService projectQueryService;

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
