package com.advertisementdesign.back.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "首条设计需求建项请求")
public record FirstRequirementRequest(
        @NotNull
        @Schema(description = "当前客户组织 ID", example = "1001")
        Long organizationId,

        @Size(max = 5000)
        @Schema(description = "设计需求说明")
        String content,

        @NotBlank
        @Size(max = 128)
        @Schema(description = "客户端消息唯一标识")
        String clientMessageId,

        @Size(max = 10)
        @Schema(description = "当前客户上传的私有草稿附件 ID")
        List<@NotNull Long> fileAssetIds) {
    public FirstRequirementRequest {
        fileAssetIds = fileAssetIds == null ? List.of() : List.copyOf(fileAssetIds);
    }
}
