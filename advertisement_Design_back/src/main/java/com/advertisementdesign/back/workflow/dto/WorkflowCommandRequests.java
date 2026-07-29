package com.advertisementdesign.back.workflow.dto;

import com.advertisementdesign.back.workflow.enums.WorkflowCommandType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class WorkflowCommandRequests {
    private WorkflowCommandRequests() {
    }

    public record Execute(
            @NotNull WorkflowCommandType command,
            @NotNull @PositiveOrZero Long version,
            @NotBlank @Size(max = 96) String requestId,
            @Size(max = 1000) String reason,
            @Schema(description = "关联业务对象类型") @Size(max = 64) String relatedObjectType,
            @Schema(description = "关联业务对象 ID") Long relatedObjectId,
            @Schema(description = "关联不可变对象版本") @PositiveOrZero Integer relatedObjectVersion
    ) {
    }
}
