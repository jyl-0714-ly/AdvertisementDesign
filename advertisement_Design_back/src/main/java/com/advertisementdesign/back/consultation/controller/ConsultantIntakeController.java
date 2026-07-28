package com.advertisementdesign.back.consultation.controller;

import com.advertisementdesign.back.consultation.model.ConsultantIntakeModels;
import com.advertisementdesign.back.consultation.model.ProjectPreparationModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.consultation.service.ConsultantIntakeService;
import com.advertisementdesign.back.consultation.service.ProjectPreparationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final ProjectPreparationService projectPreparationService;

    @Operation(summary = "获取已接待咨询的正式项目准备状态")
    @GetMapping("/designer-receptions/{intakeId}/project-preparation")
    public Result<ProjectPreparationModels.ProjectPreparation> getProjectPreparation(@PathVariable Long intakeId) {
        return Result.success(projectPreparationService.get(intakeId));
    }

    @Operation(summary = "确认已完成合同签署")
    @PostMapping("/designer-receptions/{intakeId}/project-preparation/contract-confirmation")
    public Result<ProjectPreparationModels.ProjectPreparation> confirmContract(@PathVariable Long intakeId) {
        return Result.success(projectPreparationService.confirmContract(intakeId));
    }

    @Operation(summary = "确认已收到项目首付款")
    @PostMapping("/designer-receptions/{intakeId}/project-preparation/initial-payment-confirmation")
    public Result<ProjectPreparationModels.ProjectPreparation> confirmInitialPayment(@PathVariable Long intakeId) {
        return Result.success(projectPreparationService.confirmInitialPayment(intakeId));
    }

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

    @Operation(summary = "确认收到已匹配的客户咨询")
    @PostMapping("/designer-receptions/{intakeId}/acknowledge")
    public Result<ConsultantIntakeModels.DesignerReceptionVO> acknowledgeDesignerReception(
            @PathVariable Long intakeId) {
        return Result.success(consultantIntakeService.accept(intakeId));
    }

    @Operation(summary = "接待已匹配给当前设计师的客户（兼容旧版客户端）")
    @PostMapping("/designer-receptions/{intakeId}/accept")
    public Result<ConsultantIntakeModels.DesignerReceptionVO> acceptDesignerReception(
            @PathVariable Long intakeId) {
        return Result.success(consultantIntakeService.accept(intakeId));
    }

    @Operation(summary = "创建公司客服 Agent 固定流程需求草稿")
    @PostMapping("/drafts")
    public Result<ConsultantIntakeModels.ConsultantIntakeVO> createDraft(
            @Valid @RequestBody ConsultantIntakeModels.SaveConsultantIntakeDraftRequest request) {
        return Result.success(consultantIntakeService.createDraft(request));
    }

    @Operation(summary = "更新当前客户的公司客服 Agent 需求草稿")
    @PutMapping("/{intakeId}/draft")
    public Result<ConsultantIntakeModels.ConsultantIntakeVO> updateDraft(
            @PathVariable Long intakeId,
            @Valid @RequestBody ConsultantIntakeModels.SaveConsultantIntakeDraftRequest request) {
        return Result.success(consultantIntakeService.updateDraft(intakeId, request));
    }

    @Operation(summary = "读取当前客户最近的咨询需求")
    @GetMapping("/current")
    public Result<ConsultantIntakeModels.ConsultantIntakeVO> getCurrentCustomerIntake() {
        return Result.success(consultantIntakeService.getCurrentCustomerIntake());
    }

    @Operation(summary = "确认完整需求并显式转接人工设计师")
    @PostMapping("/{intakeId}/handoff")
    public Result<ConsultantIntakeModels.ConsultantIntakeVO> handoff(@PathVariable Long intakeId) {
        return Result.success(consultantIntakeService.handoff(intakeId));
    }

    @Operation(summary = "提交完整需求并匹配设计师（兼容旧版客户端）")
    @PostMapping
    public Result<ConsultantIntakeModels.ConsultantIntakeVO> submit(
            @Valid @RequestBody ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        return Result.success(consultantIntakeService.submit(request));
    }
}
