package com.advertisementdesign.back.controller;

import com.advertisementdesign.back.api.consultant.ConsultantIntakeModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.service.ConsultantIntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ConsultantIntake", description = "项目前顾问需求收集与设计师交接接口")
@RestController
@RequestMapping("/api/consultant-intakes")
@RequiredArgsConstructor
public class ConsultantIntakeController {
    private final ConsultantIntakeService consultantIntakeService;

    @Operation(summary = "获取当前设计师的客户接待列表")
    @GetMapping("/designer-receptions")
    public Result<List<ConsultantIntakeModels.DesignerReceptionVO>> listDesignerReceptions() {
        return Result.success(consultantIntakeService.listDesignerReceptions());
    }

    @Operation(summary = "获取当前设计师的客户接待详情")
    @GetMapping("/designer-receptions/{intakeId}")
    public Result<ConsultantIntakeModels.DesignerReceptionVO> getDesignerReception(
            @PathVariable Long intakeId) {
        return Result.success(consultantIntakeService.getDesignerReception(intakeId));
    }

    @Operation(summary = "接待已匹配给当前设计师的客户")
    @PostMapping("/designer-receptions/{intakeId}/accept")
    public Result<ConsultantIntakeModels.DesignerReceptionVO> acceptDesignerReception(
            @PathVariable Long intakeId) {
        return Result.success(consultantIntakeService.accept(intakeId));
    }

    @Operation(summary = "提交完整需求并匹配设计师")
    @PostMapping
    public Result<ConsultantIntakeModels.ConsultantIntakeVO> submit(
            @Valid @RequestBody ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        return Result.success(consultantIntakeService.submit(request));
    }
}
