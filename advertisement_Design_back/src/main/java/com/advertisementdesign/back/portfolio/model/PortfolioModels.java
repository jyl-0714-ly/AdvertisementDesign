package com.advertisementdesign.back.portfolio.model;

import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.portfolio.enums.PortfolioCategory;
import com.advertisementdesign.back.portfolio.enums.PortfolioStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "作品案例相关模型")
public final class PortfolioModels {
    private PortfolioModels() {
    }

    @Schema(description = "作品案例请求")
    public record PortfolioCaseRequest(
            @NotBlank String title,
            @NotNull PortfolioCategory category,
            @NotBlank String industry,
            @NotBlank String style,
            @NotBlank String serviceType,
            @NotBlank String coverUrl,
            List<String> imageUrls,
            @NotBlank String description,
            Integer sortOrder,
            Boolean featured,
            PortfolioStatus status
    ) {
    }

    @Schema(description = "作品案例视图")
    public record PortfolioCaseVO(
            Long id,
            String title,
            PortfolioCategory category,
            String industry,
            String style,
            String serviceType,
            String coverUrl,
            List<String> imageUrls,
            String description,
            Integer sortOrder,
            Boolean featured,
            PortfolioStatus status,
            String createdAt,
            String updatedAt
    ) {
    }

    @Schema(description = "作品案例详情视图")
    public record PortfolioCaseDetailVO(
            Long id,
            String title,
            PortfolioCategory category,
            String industry,
            String style,
            String serviceType,
            String coverUrl,
            List<String> imageUrls,
            String description,
            Integer sortOrder,
            Boolean featured,
            PortfolioStatus status,
            String createdAt,
            String updatedAt
    ) {
    }

    @Schema(description = "作品案例分页")
    public record PortfolioCasePage(PageResult<PortfolioCaseVO> page) {
    }
}
