package com.advertisementdesign.back.consultation.model;

import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "顾问需求收集相关模型")
public final class ConsultantIntakeModels {
    private ConsultantIntakeModels() {
    }

    @Schema(description = "提交完整顾问需求请求")
    public record SubmitConsultantIntakeRequest(
            @NotBlank(message = "项目类型不能为空")
            @Size(max = 100, message = "项目类型不能超过 100 个字符")
            String projectType,
            @NotBlank(message = "所属行业不能为空")
            @Size(max = 100, message = "所属行业不能超过 100 个字符")
            String industry,
            @NotBlank(message = "需求描述不能为空")
            @Size(max = 2000, message = "需求描述不能超过 2000 个字符")
            String requirementDescription,
            @NotBlank(message = "预算范围不能为空")
            @Size(max = 100, message = "预算范围不能超过 100 个字符")
            String budgetRange,
            @NotBlank(message = "项目周期不能为空")
            @Size(max = 100, message = "项目周期不能超过 100 个字符")
            String projectCycle
    ) {
    }

    @Schema(description = "匹配设计师信息")
    public record MatchedDesignerVO(
            Long id,
            String nickname,
            String avatar,
            boolean online,
            List<String> specialties
    ) {
    }

    @Schema(description = "设计师客户接待中心需求卡片")
    public record DesignerReceptionVO(
            Long intakeId,
            ConsultantIntakeStatus status,
            Long customerId,
            String customerName,
            String customerAvatar,
            String projectType,
            String industry,
            String requirementDescription,
            String budgetRange,
            String projectCycle,
            int matchScore,
            String matchReason,
            String humanChatId,
            String createdAt
    ) {
    }

    @Schema(description = "顾问需求提交与设计师交接结果")
    public record ConsultantIntakeVO(
            Long intakeId,
            ConsultantIntakeStatus status,
            MatchedDesignerVO matchedDesigner,
            String humanChatId,
            List<String> greetingMessages,
            String createdAt
    ) {
    }
}
