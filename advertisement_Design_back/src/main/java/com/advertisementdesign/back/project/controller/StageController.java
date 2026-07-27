package com.advertisementdesign.back.project.controller;

import com.advertisementdesign.back.project.model.StageModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.project.enums.StageActionStatus;
import com.advertisementdesign.back.project.service.StageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Stage", description = "项目阶段接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StageController {
    private final StageService stageService;

    @Operation(summary = "发起阶段确认")
    @PostMapping("/projects/{projectId}/stages/{stageCode}/actions")
    public Result<StageModels.StageActionVO> createAction(
            @PathVariable Long projectId,
            @PathVariable String stageCode,
            @Valid @org.springframework.web.bind.annotation.RequestBody StageModels.CreateStageActionRequest request) {
        return Result.success(stageService.createAction(projectId, stageCode, request));
    }

    @Operation(summary = "确认阶段动作")
    @PostMapping("/stage-actions/{actionId}/confirm")
    public Result<StageModels.StageActionVO> confirm(
            @PathVariable Long actionId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) StageModels.StageActionResponseRequest request) {
        return Result.success(stageService.confirm(actionId, request));
    }

    @Operation(summary = "驳回阶段动作")
    @PostMapping("/stage-actions/{actionId}/reject")
    public Result<StageModels.StageActionVO> reject(
            @PathVariable Long actionId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) StageModels.StageActionResponseRequest request) {
        return Result.success(stageService.reject(actionId, request));
    }

    @Operation(summary = "阶段动作列表")
    @GetMapping("/projects/{projectId}/stage-actions")
    public Result<List<StageModels.StageActionVO>> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) String stageCode,
            @RequestParam(required = false) StageActionStatus status) {
        return Result.success(stageService.list(projectId, stageCode, status));
    }
}
