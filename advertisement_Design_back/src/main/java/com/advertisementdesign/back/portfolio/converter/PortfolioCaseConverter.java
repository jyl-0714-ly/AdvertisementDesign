package com.advertisementdesign.back.portfolio.converter;

import com.advertisementdesign.back.portfolio.entity.PortfolioCaseEntity;
import com.advertisementdesign.back.portfolio.model.PortfolioModels;

import java.util.List;

public final class PortfolioCaseConverter {
    private PortfolioCaseConverter() {
    }

    public static PortfolioModels.PortfolioCaseVO toVO(PortfolioCaseEntity entity) {
        return toVO(entity, List.of());
    }

    public static PortfolioModels.PortfolioCaseVO toVO(PortfolioCaseEntity entity, List<Long> assetFileIds) {
        return new PortfolioModels.PortfolioCaseVO(
                entity.getId(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getIndustry(),
                entity.getStyle(),
                entity.getServiceType(),
                entity.getCoverFileId(),
                assetFileIds,
                entity.getDescription(),
                entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getFeatured()),
                entity.getStatus(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }
}
