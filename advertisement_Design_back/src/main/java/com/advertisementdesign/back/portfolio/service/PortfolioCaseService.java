package com.advertisementdesign.back.portfolio.service;

import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.portfolio.converter.PortfolioCaseConverter;
import com.advertisementdesign.back.portfolio.entity.PortfolioCaseEntity;
import com.advertisementdesign.back.portfolio.enums.PortfolioCategory;
import com.advertisementdesign.back.portfolio.enums.PortfolioStatus;
import com.advertisementdesign.back.portfolio.mapper.PortfolioCaseMapper;
import com.advertisementdesign.back.portfolio.model.PortfolioModels;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioCaseService {
    private static final int MAX_BATCH_IMAGE_COUNT = 60;

    private final PortfolioCaseMapper portfolioCaseMapper;
    private final FileService fileService;
    private final CurrentActorProvider currentActorProvider;

    public PageResult<PortfolioModels.PortfolioCaseVO> list(
            PortfolioCategory category,
            String industry,
            String style,
            String keyword,
            Boolean featured,
            long page,
            long size) {
        ActorRef actor = currentActorProvider.currentActor().map(CurrentActorProvider.CurrentActor::actor).orElse(null);
        LambdaQueryWrapper<PortfolioCaseEntity> query = buildVisibleQuery(actor)
                .eq(category != null, PortfolioCaseEntity::getCategory, category)
                .eq(StringUtils.hasText(industry), PortfolioCaseEntity::getIndustry, industry)
                .eq(StringUtils.hasText(style), PortfolioCaseEntity::getStyle, style)
                .eq(featured != null, PortfolioCaseEntity::getFeatured, featured);

        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(PortfolioCaseEntity::getTitle, keyword)
                    .or().like(PortfolioCaseEntity::getDescription, keyword)
                    .or().like(PortfolioCaseEntity::getIndustry, keyword)
                    .or().like(PortfolioCaseEntity::getStyle, keyword)
                    .or().like(PortfolioCaseEntity::getServiceType, keyword));
        }
        query.orderByAsc(PortfolioCaseEntity::getSortOrder);

        long currentPage = Math.max(page, 1);
        long pageSize = Math.max(size, 1);
        Page<PortfolioCaseEntity> result = portfolioCaseMapper.selectPage(new Page<>(currentPage, pageSize), query);
        List<PortfolioModels.PortfolioCaseVO> records = result.getRecords().stream()
                .map(PortfolioCaseConverter::toVO)
                .toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    public PortfolioModels.PortfolioCaseVO detail(Long id) {
        PortfolioCaseEntity entity = portfolioCaseMapper.selectOne(
                buildVisibleQuery(currentActorProvider.currentActor()
                        .map(CurrentActorProvider.CurrentActor::actor).orElse(null))
                        .eq(PortfolioCaseEntity::getId, id));
        if (entity == null) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return PortfolioCaseConverter.toVO(entity);
    }

    public FileModels.FileAssetVO uploadPublicImage(
            MultipartFile file,
            boolean cover) {
        ensureDesigner();
        return uploadPublicImageToScene(file, cover);
    }

    public List<FileModels.FileAssetVO> uploadPublicImages(
            List<MultipartFile> files,
            boolean cover) {
        ensureDesigner();
        if (files == null || files.isEmpty()) {
            throw new ApiException(400, "请选择至少一张图片");
        }
        if (files.size() > MAX_BATCH_IMAGE_COUNT) {
            throw new ApiException(400, "单次最多上传60张图片");
        }
        return files.stream()
                .map(file -> uploadPublicImageToScene(file, cover))
                .toList();
    }

    private FileModels.FileAssetVO uploadPublicImageToScene(
            MultipartFile file,
            boolean cover) {
        return fileService.upload(file, cover
                ? StorageScene.PORTFOLIO_COVER_PUBLIC
                : StorageScene.PORTFOLIO_DETAIL_PUBLIC);
    }

    @Transactional
    public PortfolioModels.PortfolioCaseVO create(PortfolioModels.PortfolioCaseRequest request) {
        ensureDesigner();
        LocalDateTime now = LocalDateTime.now();
        PortfolioCaseEntity entity = PortfolioCaseEntity.builder()
                .title(request.title())
                .category(request.category())
                .industry(request.industry())
                .style(request.style())
                .serviceType(request.serviceType())
                .coverUrl(request.coverUrl())
                .imageUrls(request.imageUrls())
                .description(request.description())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .featured(Boolean.TRUE.equals(request.featured()))
                .status(request.status() == null ? PortfolioStatus.PUBLISHED : request.status())
                .createdAt(now)
                .updatedAt(now)
                .build();
        portfolioCaseMapper.insert(entity);
        return PortfolioCaseConverter.toVO(entity);
    }

    @Transactional
    public PortfolioModels.PortfolioCaseVO update(Long id, PortfolioModels.PortfolioCaseRequest request) {
        ensureDesigner();
        PortfolioCaseEntity entity = requireEntity(id);
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.category() != null) {
            entity.setCategory(request.category());
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
        if (request.featured() != null) {
            entity.setFeatured(request.featured());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        portfolioCaseMapper.updateById(entity);
        return PortfolioCaseConverter.toVO(entity);
    }

    @Transactional
    public boolean delete(Long id) {
        ensureDesigner();
        PortfolioCaseEntity entity = requireEntity(id);
        entity.setStatus(PortfolioStatus.OFFLINE);
        entity.setUpdatedAt(LocalDateTime.now());
        portfolioCaseMapper.updateById(entity);
        return true;
    }

    private PortfolioCaseEntity requireEntity(Long id) {
        PortfolioCaseEntity entity = portfolioCaseMapper.selectById(id);
        if (entity == null) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return entity;
    }

    private LambdaQueryWrapper<PortfolioCaseEntity> buildVisibleQuery(ActorRef actor) {
        LambdaQueryWrapper<PortfolioCaseEntity> query = new LambdaQueryWrapper<>();
        if (actor != null && actor.type() == ActorRef.ActorType.DESIGNER_USER) {
            return query;
        }
        query.eq(PortfolioCaseEntity::getStatus, PortfolioStatus.PUBLISHED);
        if (actor == null) {
            query.eq(PortfolioCaseEntity::getFeatured, true);
        }
        return query;
    }

    private void ensureDesigner() {
        if (currentActorProvider.requireCurrentActor().actor().type() != ActorRef.ActorType.DESIGNER_USER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
