package com.advertisementdesign.back.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ProjectNamingRequests {
    private ProjectNamingRequests() {
    }

    public record ManualRename(
            @Schema(description = "新的项目名称", maxLength = 100)
            @NotBlank @Size(max = 100) String name,
            @Schema(description = "当前项目对象版本")
            @NotNull @PositiveOrZero Long version) {
    }

    public record RestoreAutomatic(
            @Schema(description = "当前项目对象版本")
            @NotNull @PositiveOrZero Long version) {
    }
}
