package com.advertisementdesign.back.portfolio.service;

import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.StorageZone;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.portfolio.converter.PortfolioCaseConverter;
import com.advertisementdesign.back.portfolio.entity.PortfolioCaseAssetEntity;
import com.advertisementdesign.back.portfolio.entity.PortfolioCaseEntity;
import com.advertisementdesign.back.portfolio.enums.PortfolioCategory;
import com.advertisementdesign.back.portfolio.enums.PortfolioStatus;
import com.advertisementdesign.back.portfolio.mapper.PortfolioCaseAssetMapper;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioCaseService {
    private static final int MAX_BATCH_IMAGE_COUNT = 60;

    private final PortfolioCaseMapper portfolioCaseMapper;
    private final PortfolioCaseAssetMapper portfolioCaseAssetMapper;
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
        List<PortfolioCaseEntity> entities = result.getRecords();
        Map<Long, List<Long>> assetsByCaseId = assetFileIdsByCase(
                entities.stream().map(PortfolioCaseEntity::getId).collect(Collectors.toSet()));
        List<PortfolioModels.PortfolioCaseVO> records = entities.stream()
                .map(entity -> PortfolioCaseConverter.toVO(
                        entity, assetsByCaseId.getOrDefault(entity.getId(), List.of())))
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
        return PortfolioCaseConverter.toVO(entity, assetFileIds(entity.getId()));
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
        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        return fileService.upload(file, cover
                ? StorageScene.PORTFOLIO_COVER_PUBLIC
                : StorageScene.PORTFOLIO_DETAIL_PUBLIC,
                new FileService.Uploader(actor.type().name(), actor.actorId()));
    }

    @Transactional
    public PortfolioModels.PortfolioCaseVO create(PortfolioModels.PortfolioCaseRequest request) {
        ensureDesigner();
        validatePublicFiles(request.coverFileId(), request.assetFileIds());
        LocalDateTime now = LocalDateTime.now();
        PortfolioCaseEntity entity = PortfolioCaseEntity.builder()
                .title(request.title())
                .category(request.category())
                .industry(request.industry())
                .style(request.style())
                .serviceType(request.serviceType())
                .coverFileId(request.coverFileId())
                .description(request.description())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .featured(Boolean.TRUE.equals(request.featured()))
                .status(request.status() == null ? PortfolioStatus.PUBLISHED : request.status())
                .publishedAt(request.status() == null || request.status() == PortfolioStatus.PUBLISHED ? now : null)
                .version(0L)
                .createdBy(currentActorProvider.requireCurrentActor().actor().actorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        portfolioCaseMapper.insert(entity);
        replaceAssets(entity.getId(), request.assetFileIds());
        return PortfolioCaseConverter.toVO(entity, assetFileIds(entity.getId()));
    }

    @Transactional
    public PortfolioModels.PortfolioCaseVO update(Long id, PortfolioModels.PortfolioCaseRequest request) {
        ensureDesigner();
        validatePublicFiles(request.coverFileId(), request.assetFileIds());
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
        if (request.coverFileId() != null) {
            entity.setCoverFileId(request.coverFileId());
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
        if (request.assetFileIds() != null) {
            replaceAssets(entity.getId(), request.assetFileIds());
        }
        return PortfolioCaseConverter.toVO(entity, assetFileIds(entity.getId()));
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

    private Map<Long, List<Long>> assetFileIdsByCase(Set<Long> caseIds) {
        if (caseIds.isEmpty()) {
            return Map.of();
        }
        return portfolioCaseAssetMapper.selectList(new LambdaQueryWrapper<PortfolioCaseAssetEntity>()
                        .in(PortfolioCaseAssetEntity::getPortfolioCaseId, caseIds)
                        .orderByAsc(PortfolioCaseAssetEntity::getPortfolioCaseId)
                        .orderByAsc(PortfolioCaseAssetEntity::getDisplayOrder))
                .stream().collect(Collectors.groupingBy(
                        PortfolioCaseAssetEntity::getPortfolioCaseId,
                        Collectors.mapping(PortfolioCaseAssetEntity::getFileAssetId, Collectors.toList())));
    }

    private List<Long> assetFileIds(Long caseId) {
        return portfolioCaseAssetMapper.selectList(new LambdaQueryWrapper<PortfolioCaseAssetEntity>()
                        .eq(PortfolioCaseAssetEntity::getPortfolioCaseId, caseId)
                        .orderByAsc(PortfolioCaseAssetEntity::getDisplayOrder))
                .stream().map(PortfolioCaseAssetEntity::getFileAssetId).toList();
    }

    private void replaceAssets(Long caseId, List<Long> fileIds) {
        portfolioCaseAssetMapper.delete(new LambdaQueryWrapper<PortfolioCaseAssetEntity>()
                .eq(PortfolioCaseAssetEntity::getPortfolioCaseId, caseId));
        List<Long> ids = fileIds == null ? List.of() : fileIds.stream().distinct().toList();
        for (int index = 0; index < ids.size(); index++) {
            portfolioCaseAssetMapper.insert(PortfolioCaseAssetEntity.builder()
                    .portfolioCaseId(caseId).fileAssetId(ids.get(index)).assetRole("DETAIL")
                    .displayOrder(index).createdAt(LocalDateTime.now()).build());
        }
    }

    private void validatePublicFiles(Long coverFileId, List<Long> fileIds) {
        if (coverFileId != null) {
            requirePublicPortfolioFile(coverFileId);
        }
        if (fileIds != null) fileIds.stream().distinct().forEach(this::requirePublicPortfolioFile);
    }

    private void requirePublicPortfolioFile(Long fileId) {
        FileService.AssetMetadata metadata = fileService.requireActiveMetadata(fileId);
        if (metadata.businessScope() != FileBusinessScope.PUBLIC_PORTFOLIO
                || metadata.storageZone() != StorageZone.PUBLIC || metadata.projectId() != null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "作品案例只能引用公开作品存储区文件");
        }
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
