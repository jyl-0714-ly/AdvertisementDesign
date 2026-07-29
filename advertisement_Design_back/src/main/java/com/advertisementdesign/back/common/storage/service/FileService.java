package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.config.StorageProperties;
import com.advertisementdesign.back.common.storage.converter.FileConverter;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import com.advertisementdesign.back.common.storage.enums.StorageScene;
import com.advertisementdesign.back.common.storage.enums.StorageVisibility;
import com.advertisementdesign.back.common.storage.enums.StorageZone;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.communication.service.ConversationAccessService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.service.ProjectAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "txt", "csv",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z");
    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
            "application/x-msdownload", "application/x-sh", "application/x-httpd-php",
            "text/html", "application/xhtml+xml");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<StorageScene> IMAGE_ONLY_SCENES = Set.of(
            StorageScene.PORTFOLIO_COVER_PUBLIC,
            StorageScene.PORTFOLIO_DETAIL_PUBLIC,
            StorageScene.USER_AVATAR_PUBLIC,
            StorageScene.CONVERSATION_IMAGE);

    private final StorageRepository storageRepository;
    private final ConversationAccessService conversationAccessService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final LocalFileStorage localFileStorage;
    private final FileConverter converter;
    private final CurrentUserProfileProvider currentUserProfileProvider;
    private final Map<StorageProvider, StorageGateway> gateways = new EnumMap<>(StorageProvider.class);
    private StorageProperties storageProperties = new StorageProperties();

    public FileService(StorageRepository storageRepository,
                       ConversationAccessService conversationAccessService,
                       ProjectAuthorizationService projectAuthorizationService,
                       LocalFileStorage localFileStorage,
                       FileConverter converter,
                       CurrentUserProfileProvider currentUserProfileProvider) {
        this.storageRepository = storageRepository;
        this.conversationAccessService = conversationAccessService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.localFileStorage = localFileStorage;
        this.converter = converter;
        this.currentUserProfileProvider = currentUserProfileProvider;
        this.gateways.put(StorageProvider.LOCAL, localFileStorage);
    }

    @Autowired
    void configureStorage(StorageProperties properties, List<StorageGateway> storageGateways) {
        this.storageProperties = properties;
        storageGateways.forEach(gateway -> gateways.put(gateway.provider(), gateway));
    }

    @Transactional
    public FileModels.FileAssetVO upload(MultipartFile file) {
        return upload(file, StorageScene.GENERAL_PRIVATE);
    }

    @Transactional
    public FileModels.FileAssetVO upload(MultipartFile file, StorageScene scene) {
        Objects.requireNonNull(scene, "Storage scene is required");
        validate(file, scene);
        UserProfile currentUser = currentUserProfileProvider.currentUserProfile();
        String safeName = safeOriginalName(file.getOriginalFilename());
        byte[] content = readContent(file);
        StorageGateway gateway = activeGateway();
        StorageVisibility visibility = scene.getVisibility();
        String storageName = UUID.randomUUID() + "-" + safeName;
        String objectKey = scene.getKeySegment() + "/" + currentUser.id() + "/" + storageName;
        String bucketName = gateway.bucketName(visibility);
        LocalDateTime now = LocalDateTime.now();
        FileAssetEntity asset = FileAssetEntity.builder()
                .uploaderActorType(actorType(currentUser))
                .uploaderActorId(currentUser.id())
                .organizationId(null)
                .projectId(null)
                .businessScope(scene.isPublic()
                        ? FileBusinessScope.PUBLIC_PORTFOLIO
                        : FileBusinessScope.PRIVATE_DRAFT)
                .visibility(scene.isPublic()
                        ? StorageVisibility.PUBLIC
                        : StorageVisibility.INTERNAL)
                .originalName(safeName)
                .storageProvider(gateway.provider())
                .storageZone(scene.isPublic() ? StorageZone.PUBLIC : StorageZone.PRIVATE)
                .bucketName(bucketName)
                .objectKey(objectKey)
                .mimeType(normalizeMimeType(file.getContentType()))
                .fileExtension(extensionOf(safeName))
                .fileSize((long) content.length)
                .hashAlgorithm("SHA256")
                .fileHash(sha256(content))
                .legalHold(false)
                .status(FileStatus.ACTIVE)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            gateway.store(bucketName, objectKey, new ByteArrayInputStream(content), content.length,
                    asset.getMimeType(), visibility);
            return converter.toVO(storageRepository.save(asset));
        } catch (IOException exception) {
            compensateDelete(gateway, bucketName, objectKey, exception);
            throw new UncheckedIOException("Failed to store uploaded file", exception);
        } catch (RuntimeException exception) {
            compensateDelete(gateway, bucketName, objectKey, exception);
            throw exception;
        }
    }

    public FileModels.FileAssetVO detail(Long fileId) {
        return converter.toVO(findAccessibleAsset(fileId));
    }

    public byte[] download(Long fileId) {
        FileAssetEntity asset = findAccessibleAsset(fileId);
        try {
            return gatewayFor(asset.getStorageProvider()).read(asset.getBucketName(), asset.getObjectKey());
        } catch (IOException | RuntimeException exception) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
    }

    @Transactional
    public boolean delete(Long fileId) {
        FileAssetEntity asset = storageRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserProfile currentUser = currentUserProfileProvider.currentUserProfile();
        if (!Objects.equals(asset.getUploaderActorId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        try {
            gatewayFor(asset.getStorageProvider()).delete(asset.getBucketName(), asset.getObjectKey());
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete stored file", exception);
        } catch (RuntimeException exception) {
            throw exception;
        }
        asset.setStatus(FileStatus.DELETED);
        asset.setUpdatedAt(LocalDateTime.now());
        storageRepository.save(asset);
        return true;
    }

    private StorageGateway activeGateway() {
        StorageProvider provider = storageProperties.getProvider();
        if (provider == StorageProvider.OSS && !storageProperties.getOss().isEnabled()) {
            throw new IllegalStateException("OSS storage provider requires app.storage.oss.enabled=true");
        }
        return gatewayFor(provider);
    }

    private StorageGateway gatewayFor(StorageProvider provider) {
        StorageProvider effectiveProvider = provider == null ? StorageProvider.LOCAL : provider;
        StorageGateway gateway = gateways.get(effectiveProvider);
        if (gateway == null) {
            throw new IllegalStateException("Storage provider is not available: " + effectiveProvider);
        }
        return gateway;
    }

    private void validate(MultipartFile file, StorageScene scene) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        if (file.getSize() > storageProperties.getMaxFileSize()) {
            throw new ApiException(400, "文件大小超过限制");
        }
        String safeName = safeOriginalName(file.getOriginalFilename());
        int extensionIndex = safeName.lastIndexOf('.');
        String extension = extensionIndex < 0 ? "" : safeName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(400, "不支持的文件类型");
        }
        String mimeType = normalizeMimeType(file.getContentType());
        if (!StringUtils.hasText(mimeType) || BLOCKED_MIME_TYPES.contains(mimeType)) {
            throw new ApiException(400, "不支持的文件类型");
        }
        if (IMAGE_ONLY_SCENES.contains(scene)
                && (!IMAGE_EXTENSIONS.contains(extension) || !IMAGE_MIME_TYPES.contains(mimeType))) {
            throw new ApiException(400, "该上传场景仅支持 JPG、PNG、GIF 或 WebP 图片");
        }
    }

    private String safeOriginalName(String originalName) {
        String candidate = StringUtils.hasText(originalName) ? originalName : "file.bin";
        String normalized = candidate.replace('\\', '/');
        String safeName = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "");
        if (!StringUtils.hasText(safeName) || safeName.length() > 255 || ".".equals(safeName) || "..".equals(safeName)) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        return safeName;
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read uploaded file", exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String extensionOf(String safeName) {
        int index = safeName.lastIndexOf('.');
        return index < 0 ? null : safeName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private ActorRef.ActorType actorType(UserProfile user) {
        return switch (user.role()) {
            case CUSTOMER -> ActorRef.ActorType.CUSTOMER_USER;
            case DESIGNER -> ActorRef.ActorType.DESIGNER_USER;
            case ADMIN -> ActorRef.ActorType.ADMIN_USER;
        };
    }

    private String normalizeMimeType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private void compensateDelete(StorageGateway gateway, String bucketName, String objectKey, Exception original) {
        try {
            gateway.delete(bucketName, objectKey);
        } catch (Exception cleanupException) {
            original.addSuppressed(cleanupException);
        }
    }

    private FileAssetEntity findAccessibleAsset(Long fileId) {
        FileAssetEntity asset = storageRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (asset.getStatus() == FileStatus.DELETED) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        UserProfile currentUser = currentUserProfileProvider.currentUserProfile();
        var projectAccess = projectAuthorizationService.authorizeProjectFile(fileId);
        boolean associated = conversationAccessService.isAttachedToConversation(fileId) || projectAccess.projectFile();
        if (associated) {
            if (!canAccessAssociatedFile(fileId, projectAccess)) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
        } else if (!Objects.equals(asset.getUploaderActorId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return asset;
    }

    private boolean canAccessAssociatedFile(Long fileId,
                                            ProjectAuthorizationService.ProjectFileAccessDecision projectAccess) {
        return conversationAccessService.canCurrentUserAccessAttachedFile(fileId) || projectAccess.allowed();
    }
}
