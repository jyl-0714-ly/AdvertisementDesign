package com.advertisementdesign.back.controller;

import com.advertisementdesign.back.api.portfolio.PortfolioModels;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.service.PortfolioCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PortfolioCase", description = "作品案例接口")
@RestController
@RequestMapping("/api/portfolio-cases")
@RequiredArgsConstructor
public class PortfolioCaseController {
    private final PortfolioCaseService portfolioCaseService;

    @Operation(summary = "作品案例列表")
    @GetMapping
    public Result<PageResult<PortfolioModels.PortfolioCaseVO>> list(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(portfolioCaseService.list(industry, style, serviceType, keyword, page, size));
    }

    @Operation(summary = "作品案例详情")
    @GetMapping("/{id}")
    public Result<PortfolioModels.PortfolioCaseVO> detail(@PathVariable Long id) {
        return Result.success(portfolioCaseService.detail(id));
    }

    @Operation(summary = "新增作品案例")
    @PostMapping
    public Result<PortfolioModels.PortfolioCaseVO> create(@Valid @org.springframework.web.bind.annotation.RequestBody PortfolioModels.PortfolioCaseRequest request) {
        return Result.success(portfolioCaseService.create(request));
    }

    @Operation(summary = "更新作品案例")
    @PutMapping("/{id}")
    public Result<PortfolioModels.PortfolioCaseVO> update(
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody PortfolioModels.PortfolioCaseRequest request) {
        return Result.success(portfolioCaseService.update(id, request));
    }

    @Operation(summary = "删除或下线作品案例")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(portfolioCaseService.delete(id));
    }
}
