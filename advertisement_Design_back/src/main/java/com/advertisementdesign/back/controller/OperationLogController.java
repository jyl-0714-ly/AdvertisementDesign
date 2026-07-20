package com.advertisementdesign.back.controller;

import com.advertisementdesign.back.api.operation.OperationLogModels;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OperationLog", description = "操作日志接口")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class OperationLogController {
    private final OperationLogService operationLogService;

    @Operation(summary = "项目操作日志")
    @GetMapping("/{projectId}/operation-logs")
    public Result<PageResult<OperationLogModels.OperationLogVO>> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(operationLogService.list(projectId, bizType, action, page, size));
    }
}
