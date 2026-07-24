package com.advertisementdesign.back.controller;

import com.advertisementdesign.back.api.consultant.ConsultantIntakeModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.service.ConsultantIntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ConsultantIntake", description = "项目前顾问需求收集与设计师交接接口")
@RestController
@RequestMapping("/api/consultant-intakes")
@RequiredArgsConstructor
public class ConsultantIntakeController {
    private final ConsultantIntakeService consultantIntakeService;

    @Operation(summary = "提交完整需求并匹配设计师")
    @PostMapping
    public Result<ConsultantIntakeModels.ConsultantIntakeVO> submit(
            @Valid @RequestBody ConsultantIntakeModels.SubmitConsultantIntakeRequest request) {
        return Result.success(consultantIntakeService.submit(request));
    }
}
