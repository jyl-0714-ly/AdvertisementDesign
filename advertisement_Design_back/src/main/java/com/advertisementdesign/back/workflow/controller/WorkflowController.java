package com.advertisementdesign.back.workflow.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.workflow.model.WorkflowModels;
import com.advertisementdesign.back.workflow.service.WorkflowQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Project Workflow", description = "项目七阶段查询")
@RestController
@RequestMapping("/api/projects/{projectId}/stages")
@RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowQueryService queryService;

    @Operation(summary = "查询项目七阶段")
    @GetMapping
    public Result<List<WorkflowModels.StageInstanceView>> stages(@PathVariable Long projectId) {
        return Result.success(queryService.stages(projectId));
    }

    @Operation(summary = "查询当前阶段工作台投影")
    @GetMapping("/current")
    public Result<WorkflowModels.CurrentStageWorkspaceView> current(@PathVariable Long projectId) {
        return Result.success(queryService.currentStage(projectId));
    }

    @Operation(summary = "查询阶段历史事件")
    @GetMapping("/{stageInstanceId}/events")
    public Result<List<WorkflowModels.StageEventView>> history(
            @PathVariable Long projectId, @PathVariable Long stageInstanceId) {
        return Result.success(queryService.history(projectId, stageInstanceId));
    }
}
