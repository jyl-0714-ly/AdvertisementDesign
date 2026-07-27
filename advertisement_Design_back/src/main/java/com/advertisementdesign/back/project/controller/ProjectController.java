package com.advertisementdesign.back.project.controller;

import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Project", description = "项目接口")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @Operation(summary = "当前用户项目列表")
    @GetMapping
    public Result<PageResult<ProjectModels.ProjectVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currentStage,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(projectService.list(status, currentStage, keyword, page, size));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    public Result<ProjectModels.ProjectVO> detail(@PathVariable Long id) {
        return Result.success(projectService.detail(id));
    }

    @Operation(summary = "新增项目")
    @PostMapping
    public Result<ProjectModels.ProjectVO> create(@Valid @org.springframework.web.bind.annotation.RequestBody ProjectModels.CreateProjectRequest request) {
        return Result.success(projectService.create(request));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    public Result<ProjectModels.ProjectVO> update(
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody ProjectModels.UpdateProjectRequest request) {
        return Result.success(projectService.update(id, request));
    }

    @Operation(summary = "删除或取消项目")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(projectService.delete(id));
    }

    @Operation(summary = "项目阶段列表")
    @GetMapping("/{projectId}/stages")
    public Result<List<ProjectModels.ProjectStageVO>> stages(@PathVariable Long projectId) {
        return Result.success(projectService.stages(projectId));
    }
}
