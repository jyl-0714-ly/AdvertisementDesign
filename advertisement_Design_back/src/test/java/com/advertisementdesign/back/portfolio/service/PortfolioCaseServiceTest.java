package com.advertisementdesign.back.portfolio.service;

import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.enums.StorageVisibility;
import com.advertisementdesign.back.common.storage.enums.StorageZone;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.portfolio.entity.PortfolioCaseEntity;
import com.advertisementdesign.back.portfolio.enums.PortfolioCategory;
import com.advertisementdesign.back.portfolio.enums.PortfolioStatus;
import com.advertisementdesign.back.portfolio.mapper.PortfolioCaseAssetMapper;
import com.advertisementdesign.back.portfolio.mapper.PortfolioCaseMapper;
import com.advertisementdesign.back.portfolio.model.PortfolioModels;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioCaseServiceTest {
    @Mock
    private PortfolioCaseMapper portfolioCaseMapper;
    @Mock
    private PortfolioCaseAssetMapper portfolioCaseAssetMapper;
    @Mock
    private FileService fileService;
    @Mock
    private CurrentActorProvider currentActorProvider;

    private PortfolioCaseService portfolioCaseService;

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "portfolio-test");
        assistant.setCurrentNamespace(PortfolioCaseMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, PortfolioCaseEntity.class);
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(currentActorProvider.currentActor())
                .thenAnswer(invocation -> currentActorFromSecurityContext());
        org.mockito.Mockito.lenient().when(currentActorProvider.requireCurrentActor())
                .thenAnswer(invocation -> currentActorFromSecurityContext()
                        .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED)));
        org.mockito.Mockito.lenient().when(fileService.requireActiveMetadata(any(Long.class)))
                .thenAnswer(invocation -> publicPortfolioMetadata(invocation.getArgument(0)));
        portfolioCaseService = new PortfolioCaseService(
                portfolioCaseMapper, portfolioCaseAssetMapper, fileService, currentActorProvider);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousListRestrictsResultsBeforePaginationAndAppliesFilters() {
        mockPageResult(List.of(entity(1L, PortfolioStatus.PUBLISHED, true)), 1L);

        PageResult<PortfolioModels.PortfolioCaseVO> result = portfolioCaseService.list(
                PortfolioCategory.BRAND, "餐饮", "极简", "咖啡", true, 1, 10);

        Wrapper<PortfolioCaseEntity> wrapper = capturedListWrapper();
        String sql = wrapper.getSqlSegment();
        Map<String, Object> parameters = parameters(wrapper);
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("featured"));
        assertTrue(sql.contains("category"));
        assertTrue(sql.contains("industry"));
        assertTrue(sql.contains("style"));
        assertTrue(sql.contains("title") && sql.contains("description") && sql.contains("service_type"));
        assertTrue(sql.contains("ORDER BY sort_order ASC"));
        assertTrue(parameters.containsValue(PortfolioStatus.PUBLISHED));
        assertTrue(parameters.containsValue(Boolean.TRUE));
        assertTrue(parameters.containsValue(PortfolioCategory.BRAND));
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void customerListSeesAllPublishedCasesButNotDraftsOrOfflineCases() {
        authenticate(UserRole.CUSTOMER);
        mockPageResult(List.of(entity(1L, PortfolioStatus.PUBLISHED, false)), 1L);

        portfolioCaseService.list(null, null, null, null, null, 1, 10);

        Wrapper<PortfolioCaseEntity> wrapper = capturedListWrapper();
        assertTrue(wrapper.getSqlSegment().contains("status"));
        assertFalse(wrapper.getSqlSegment().contains("featured"));
    }

    @Test
    void designerListDoesNotAddVisibilityRestrictions() {
        authenticate(UserRole.DESIGNER);
        mockPageResult(List.of(entity(2L, PortfolioStatus.DRAFT, false)), 1L);

        portfolioCaseService.list(null, null, null, null, null, 1, 10);

        Wrapper<PortfolioCaseEntity> wrapper = capturedListWrapper();
        assertFalse(wrapper.getSqlSegment().contains("status"));
        assertFalse(wrapper.getSqlSegment().contains("featured"));
        assertTrue(wrapper.getSqlSegment().contains("ORDER BY sort_order ASC"));
    }

    @Test
    void invisibleOrMissingDetailReturnsNotFound() {
        when(portfolioCaseMapper.selectOne(any())).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class, () -> portfolioCaseService.detail(99L));

        assertEquals(404, exception.getCode());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<PortfolioCaseEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(portfolioCaseMapper).selectOne(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("featured"));
        assertTrue(sql.contains("id"));
    }

    @Test
    void designerUploadsPortfolioImagesToControlledPublicScenes() {
        authenticate(UserRole.DESIGNER);
        MockMultipartFile cover = new MockMultipartFile(
                "file", "cover.webp", "image/webp", new byte[]{1});
        MockMultipartFile detail = new MockMultipartFile(
                "file", "detail.jpg", "image/jpeg", new byte[]{2});

        portfolioCaseService.uploadPublicImage(cover, true);
        portfolioCaseService.uploadPublicImage(detail, false);

        verify(fileService).upload(org.mockito.ArgumentMatchers.eq(cover),
                org.mockito.ArgumentMatchers.eq(StorageScene.PORTFOLIO_COVER_PUBLIC), any(FileService.Uploader.class));
        verify(fileService).upload(org.mockito.ArgumentMatchers.eq(detail),
                org.mockito.ArgumentMatchers.eq(StorageScene.PORTFOLIO_DETAIL_PUBLIC), any(FileService.Uploader.class));
    }

    @Test
    void designerBatchUploadsPortfolioImagesToControlledPublicScene() {
        authenticate(UserRole.DESIGNER);
        MockMultipartFile first = new MockMultipartFile(
                "files", "brand-01-01.webp", "image/webp", new byte[]{1});
        MockMultipartFile second = new MockMultipartFile(
                "files", "brand-01-02.webp", "image/webp", new byte[]{2});

        portfolioCaseService.uploadPublicImages(List.of(first, second), false);

        verify(fileService).upload(org.mockito.ArgumentMatchers.eq(first),
                org.mockito.ArgumentMatchers.eq(StorageScene.PORTFOLIO_DETAIL_PUBLIC), any(FileService.Uploader.class));
        verify(fileService).upload(org.mockito.ArgumentMatchers.eq(second),
                org.mockito.ArgumentMatchers.eq(StorageScene.PORTFOLIO_DETAIL_PUBLIC), any(FileService.Uploader.class));
    }

    @Test
    void batchUploadRejectsEmptyFileList() {
        authenticate(UserRole.DESIGNER);

        ApiException exception = assertThrows(ApiException.class,
                () -> portfolioCaseService.uploadPublicImages(List.of(), false));

        assertEquals(400, exception.getCode());
        verify(fileService, never()).upload(any(), any(StorageScene.class), any(FileService.Uploader.class));
    }

    @Test
    void customerCannotUploadPortfolioImage() {
        authenticate(UserRole.CUSTOMER);
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", new byte[]{1});

        ApiException exception = assertThrows(ApiException.class,
                () -> portfolioCaseService.uploadPublicImage(file, true));

        assertEquals(403, exception.getCode());
        verify(fileService, never()).upload(any(), any(StorageScene.class), any(FileService.Uploader.class));
    }

    @Test
    void createUsesDefaultsAndPersistsWithGeneratedId() {
        authenticate(UserRole.DESIGNER);
        when(portfolioCaseMapper.insert(any())).thenAnswer(invocation -> {
            PortfolioCaseEntity entity = invocation.getArgument(0);
            entity.setId(7L);
            return 1;
        });
        PortfolioModels.PortfolioCaseRequest request = request(null, null, null);

        PortfolioModels.PortfolioCaseVO result = portfolioCaseService.create(request);

        ArgumentCaptor<PortfolioCaseEntity> captor = ArgumentCaptor.forClass(PortfolioCaseEntity.class);
        verify(portfolioCaseMapper).insert(captor.capture());
        PortfolioCaseEntity persisted = captor.getValue();
        assertEquals(0, persisted.getSortOrder());
        assertFalse(persisted.getFeatured());
        assertEquals(PortfolioStatus.PUBLISHED, persisted.getStatus());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
        assertEquals(7L, result.id());
    }

    @Test
    void updateChangesProvidedFieldsAndPersistsExistingEntity() {
        authenticate(UserRole.DESIGNER);
        PortfolioCaseEntity existing = entity(1L, PortfolioStatus.DRAFT, false);
        when(portfolioCaseMapper.selectById(1L)).thenReturn(existing);
        PortfolioModels.PortfolioCaseRequest request = request(8, true, PortfolioStatus.PUBLISHED);

        PortfolioModels.PortfolioCaseVO result = portfolioCaseService.update(1L, request);

        assertEquals("更新后的案例", existing.getTitle());
        assertEquals(8, existing.getSortOrder());
        assertTrue(existing.getFeatured());
        assertEquals(PortfolioStatus.PUBLISHED, existing.getStatus());
        verify(portfolioCaseMapper).updateById(existing);
        assertEquals("更新后的案例", result.title());
    }

    @Test
    void deleteSoftDeletesBySettingOfflineStatus() {
        authenticate(UserRole.DESIGNER);
        PortfolioCaseEntity existing = entity(1L, PortfolioStatus.PUBLISHED, true);
        when(portfolioCaseMapper.selectById(1L)).thenReturn(existing);

        assertTrue(portfolioCaseService.delete(1L));

        assertEquals(PortfolioStatus.OFFLINE, existing.getStatus());
        verify(portfolioCaseMapper).updateById(existing);
    }

    @Test
    void anonymousAndCustomerMutationsAreRejected() {
        ApiException unauthorized = assertThrows(ApiException.class,
                () -> portfolioCaseService.create(request(null, null, null)));
        assertEquals(401, unauthorized.getCode());

        authenticate(UserRole.CUSTOMER);
        ApiException forbidden = assertThrows(ApiException.class,
                () -> portfolioCaseService.delete(1L));
        assertEquals(403, forbidden.getCode());
        verify(portfolioCaseMapper, never()).insert(any());
        verify(portfolioCaseMapper, never()).updateById(any());
    }

    @Test
    void updateMissingCaseReturnsNotFound() {
        authenticate(UserRole.DESIGNER);
        when(portfolioCaseMapper.selectById(404L)).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class,
                () -> portfolioCaseService.update(404L, request(1, false, PortfolioStatus.DRAFT)));

        assertEquals(404, exception.getCode());
        verify(portfolioCaseMapper, never()).updateById(any());
    }

    private void mockPageResult(List<PortfolioCaseEntity> records, long total) {
        when(portfolioCaseMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<PortfolioCaseEntity> page = invocation.getArgument(0);
            page.setRecords(records);
            page.setTotal(total);
            return page;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parameters(Wrapper<PortfolioCaseEntity> wrapper) {
        return ((AbstractWrapper<PortfolioCaseEntity, ?, ?>) wrapper)
                .getParamNameValuePairs();
    }

    private Wrapper<PortfolioCaseEntity> capturedListWrapper() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<PortfolioCaseEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(portfolioCaseMapper).selectPage(any(Page.class), captor.capture());
        return captor.getValue();
    }

    private void authenticate(UserRole role) {
        CurrentUser currentUser = CurrentUser.builder()
                .id(10L)
                .email("portfolio-test@example.com")
                .nickname("测试用户")
                .role(role)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
    }

    private java.util.Optional<CurrentActorProvider.CurrentActor> currentActorFromSecurityContext() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CurrentUser currentUser)) {
            return java.util.Optional.empty();
        }
        ActorRef actor = new ActorRef(switch (currentUser.getRole()) {
            case CUSTOMER -> ActorRef.ActorType.CUSTOMER_USER;
            case DESIGNER -> ActorRef.ActorType.DESIGNER_USER;
            case ADMIN -> ActorRef.ActorType.ADMIN_USER;
        }, currentUser.getId());
        return java.util.Optional.of(new CurrentActorProvider.CurrentActor(actor, currentUser.getNickname()));
    }

    private FileService.AssetMetadata publicPortfolioMetadata(Long fileId) {
        return new FileService.AssetMetadata(
                fileId,
                ActorRef.ActorType.DESIGNER_USER.name(),
                10L,
                null,
                null,
                FileBusinessScope.PUBLIC_PORTFOLIO,
                StorageVisibility.PUBLIC,
                StorageZone.PUBLIC,
                "portfolio-" + fileId + ".webp",
                "image/webp",
                "webp",
                1L
        );
    }

    private PortfolioCaseEntity entity(Long id, PortfolioStatus status, boolean featured) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 24, 10, 0);
        return PortfolioCaseEntity.builder()
                .id(id)
                .title("案例")
                .category(PortfolioCategory.BRAND)
                .industry("餐饮")
                .style("极简")
                .serviceType("品牌设计")
                .coverFileId(11L)
                .description("案例描述")
                .sortOrder(1)
                .featured(featured)
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private PortfolioModels.PortfolioCaseRequest request(
            Integer sortOrder,
            Boolean featured,
            PortfolioStatus status) {
        return new PortfolioModels.PortfolioCaseRequest(
                "更新后的案例",
                PortfolioCategory.BRAND,
                "餐饮",
                "极简",
                "品牌设计",
                11L,
                List.of(12L),
                "更新后的案例描述",
                sortOrder,
                featured,
                status
        );
    }
}
