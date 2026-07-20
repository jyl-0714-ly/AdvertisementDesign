package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.portfolio.PortfolioModels;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.domain.entity.PortfolioCaseEntity;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioCaseService {
    private final DemoDataStore store;
    private final ApiAssembler assembler;
    private final AuthService authService;

    public PageResult<PortfolioModels.PortfolioCaseVO> list(String industry, String style, String serviceType, String keyword, long page, long size) {
        List<PortfolioModels.PortfolioCaseVO> records = store.listPortfolioCases(industry, style, serviceType, keyword).stream()
                .map(assembler::toPortfolioCaseVO)
                .skip(Math.max(page - 1, 0) * size)
                .limit(size)
                .toList();
        long total = store.listPortfolioCases(industry, style, serviceType, keyword).size();
        return PageResult.of(records, total, page, size);
    }

    public PortfolioModels.PortfolioCaseVO detail(Long id) {
        return store.findPortfolioCaseById(id)
                .map(assembler::toPortfolioCaseVO)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    public PortfolioModels.PortfolioCaseVO create(PortfolioModels.PortfolioCaseRequest request) {
        ensureDesigner();
        PortfolioCaseEntity entity = PortfolioCaseEntity.builder()
                .title(request.title())
                .industry(request.industry())
                .style(request.style())
                .serviceType(request.serviceType())
                .coverUrl(request.coverUrl())
                .imageUrls(request.imageUrls())
                .description(request.description())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .status(request.status() == null ? com.advertisementdesign.back.domain.enums.PortfolioStatus.PUBLISHED : request.status())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return assembler.toPortfolioCaseVO(store.savePortfolioCase(entity));
    }

    public PortfolioModels.PortfolioCaseVO update(Long id, PortfolioModels.PortfolioCaseRequest request) {
        ensureDesigner();
        PortfolioCaseEntity entity = store.findPortfolioCaseById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.industry() != null) {
            entity.setIndustry(request.industry());
        }
        if (request.style() != null) {
            entity.setStyle(request.style());
        }
        if (request.serviceType() != null) {
            entity.setServiceType(request.serviceType());
        }
        if (request.coverUrl() != null) {
            entity.setCoverUrl(request.coverUrl());
        }
        if (request.imageUrls() != null) {
            entity.setImageUrls(request.imageUrls());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return assembler.toPortfolioCaseVO(store.savePortfolioCase(entity));
    }

    public boolean delete(Long id) {
        ensureDesigner();
        PortfolioCaseEntity entity = store.findPortfolioCaseById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        entity.setStatus(com.advertisementdesign.back.domain.enums.PortfolioStatus.OFFLINE);
        entity.setUpdatedAt(LocalDateTime.now());
        store.savePortfolioCase(entity);
        return true;
    }

    private void ensureDesigner() {
        if (authService.currentUserEntity().getRole() != UserRole.DESIGNER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
