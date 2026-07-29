package com.advertisementdesign.back.common.storage.service;

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
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
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

/**
 * Neutral physical-storage and file metadata service. Business modules must authorize and establish
 * their own relations before calling this service; this class deliberately has no project,
 * communication, portfolio, or identity service dependencies.
 */
@Service
public class FileService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "txt", "csv",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z");
    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
            "application/x-msdownload", "application/x-sh", "application/x-httpd-php",
            "text/html", "application/xhtml+xml");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> IMAGE_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<StorageScene> IMAGE_ONLY_SCENES = Set.of(
            StorageScene.PORTFOLIO_COVER_PUBLIC, StorageScene.PORTFOLIO_DETAIL_PUBLIC,
            StorageScene.USER_AVATAR_PUBLIC, StorageScene.CONVERSATION_IMAGE);

    private final StorageRepository storageRepository;
    private final LocalFileStorage localFileStorage;
    private final FileConverter converter;
    private final Map<StorageProvider, StorageGateway> gateways = new EnumMap<>(StorageProvider.class);
    private StorageProperties storageProperties = new StorageProperties();

    public FileService(StorageRepository storageRepository, LocalFileStorage localFileStorage, FileConverter converter) {
        this.storageRepository = storageRepository;
        this.localFileStorage = localFileStorage;
        this.converter = converter;
        this.gateways.put(StorageProvider.LOCAL, localFileStorage);
    }

    @Autowired
    void configureStorage(StorageProperties properties, List<StorageGateway> storageGateways) {
        this.storageProperties = properties;
        storageGateways.forEach(gateway -> gateways.put(gateway.provider(), gateway));
    }

    @Transactional
    public FileModels.FileAssetVO upload(MultipartFile file, StorageScene scene, Uploader uploader) {
        Objects.requireNonNull(scene, "Storage scene is required");
        Objects.requireNonNull(uploader, "Uploader is required");
        validate(file, scene);
        String safeName = safeOriginalName(file.getOriginalFilename());
        StorageGateway gateway = activeGateway();
        StorageVisibility visibility = scene.getVisibility();
        String objectKey = scene.getKeySegment() + "/" + uploader.actorId() + "/" + UUID.randomUUID() + "-" + safeName;
        String bucketName = gateway.bucketName(visibility);
        MessageDigest digest = sha256Digest();
        try (InputStream input = file.getInputStream(); DigestInputStream hashing = new DigestInputStream(input, digest)) {
            gateway.store(bucketName, objectKey, hashing, file.getSize(), normalizeMimeType(file.getContentType()), visibility);
            LocalDateTime now = LocalDateTime.now();
            FileAssetEntity asset = FileAssetEntity.builder()
                    .uploaderActorType(uploader.actorType())
                    .uploaderActorId(uploader.actorId())
                    .businessScope(scene.isPublic() ? FileBusinessScope.PUBLIC_PORTFOLIO : FileBusinessScope.PRIVATE_DRAFT)
                    .visibility(scene.isPublic() ? StorageVisibility.PUBLIC : StorageVisibility.INTERNAL)
                    .originalName(safeName).storageProvider(gateway.provider())
                    .storageZone(scene.isPublic() ? StorageZone.PUBLIC : StorageZone.PRIVATE)
                    .bucketName(bucketName).objectKey(objectKey).mimeType(normalizeMimeType(file.getContentType()))
                    .fileExtension(extensionOf(safeName)).fileSize(file.getSize()).hashAlgorithm("SHA256")
                    .fileHash(HexFormat.of().formatHex(digest.digest())).legalHold(false).status(FileStatus.ACTIVE)
                    .version(0L).createdAt(now).updatedAt(now).build();
            return converter.toVO(storageRepository.save(asset));
        } catch (IOException exception) {
            compensateDelete(gateway, bucketName, objectKey, exception);
            throw new UncheckedIOException("Failed to stream uploaded file", exception);
        } catch (RuntimeException exception) {
            compensateDelete(gateway, bucketName, objectKey, exception);
            throw exception;
        }
    }

    FileAssetEntity requireActiveAsset(Long fileId) {
        FileAssetEntity asset = storageRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (asset.getStatus() != FileStatus.ACTIVE) throw new ApiException(ApiErrorCode.NOT_FOUND);
        return asset;
    }

    public FileModels.FileAssetVO metadata(Long fileId) {
        return converter.toVO(requireActiveAsset(fileId));
    }

    public InputStream openStream(Long fileId) {
        FileAssetEntity asset = requireActiveAsset(fileId);
        try {
            return gatewayFor(asset.getStorageProvider()).openStream(asset.getBucketName(), asset.getObjectKey());
        } catch (IOException | RuntimeException exception) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
    }

    @Transactional
    public boolean deleteOwnedDraft(Long fileId, Uploader actor) {
        FileAssetEntity asset = requireActiveAsset(fileId);
        if (asset.getBusinessScope() != FileBusinessScope.PRIVATE_DRAFT
                || !Objects.equals(asset.getUploaderActorType(), actor.actorType())
                || !Objects.equals(asset.getUploaderActorId(), actor.actorId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        try {
            gatewayFor(asset.getStorageProvider()).delete(asset.getBucketName(), asset.getObjectKey());
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete stored file", exception);
        }
        asset.setStatus(FileStatus.DELETED);
        asset.setUpdatedAt(LocalDateTime.now());
        storageRepository.save(asset);
        return true;
    }

    public record Uploader(String actorType, Long actorId) {
        public Uploader {
            Objects.requireNonNull(actorType, "Uploader actor type is required");
            Objects.requireNonNull(actorId, "Uploader actor id is required");
        }
    }

    public record AssetMetadata(Long id, String uploaderActorType, Long uploaderActorId,
                                Long organizationId, Long projectId, FileBusinessScope businessScope,
                                StorageVisibility visibility, StorageZone storageZone,
                                String originalName, String mimeType, String fileExtension, Long fileSize) {
    }

    public AssetMetadata requireActiveMetadata(Long fileId) {
        FileAssetEntity asset = requireActiveAsset(fileId);
        return toMetadata(asset);
    }

    @Transactional
    public AssetMetadata assignOrganization(Long fileId, Long organizationId) {
        Objects.requireNonNull(organizationId, "Organization id is required");
        FileAssetEntity asset = requireActiveAsset(fileId);
        if (asset.getBusinessScope() != FileBusinessScope.PRIVATE_DRAFT
                || asset.getProjectId() != null
                || asset.getStorageZone() != StorageZone.PRIVATE
                || asset.getOrganizationId() != null && !Objects.equals(asset.getOrganizationId(), organizationId)) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "文件状态已变化，请重新上传");
        }
        asset.setOrganizationId(organizationId);
        asset.setUpdatedAt(LocalDateTime.now());
        return toMetadata(storageRepository.save(asset));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void claimProjectMessageDraft(Long fileId, Uploader uploader, Long organizationId, Long projectId) {
        FileAssetEntity asset = requireActiveAsset(fileId);
        if (!Objects.equals(asset.getUploaderActorType(), uploader.actorType())
                || !Objects.equals(asset.getUploaderActorId(), uploader.actorId())
                || !Objects.equals(asset.getOrganizationId(), organizationId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "不能跨用户、组织或项目关联附件");
        }
        if (asset.getBusinessScope() != FileBusinessScope.PRIVATE_DRAFT || asset.getProjectId() != null
                || !storageRepository.claimProjectMessageDraft(asset, uploader.actorType(), uploader.actorId(), organizationId, projectId)) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "附件状态已变化，请重新选择");
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void claimFirstRequirementDraft(Long fileId, Uploader uploader, Long organizationId, Long projectId) {
        FileAssetEntity asset = requireActiveAsset(fileId);
        if (!"CUSTOMER_USER".equals(uploader.actorType())
                || !Objects.equals(asset.getUploaderActorType(), uploader.actorType())
                || !Objects.equals(asset.getUploaderActorId(), uploader.actorId())
                || asset.getBusinessScope() != FileBusinessScope.PRIVATE_DRAFT || asset.getProjectId() != null) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "不能关联其他客户上传的附件");
        }
        if (!storageRepository.claimFirstRequirementDraft(asset, uploader.actorId(), organizationId, projectId)) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "附件状态已变化，请重新选择");
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void claimProjectArtifactDraft(Long fileId, Uploader uploader, Long organizationId, Long projectId) {
        FileAssetEntity asset = requireActiveAsset(fileId);
        if (!Objects.equals(asset.getUploaderActorType(), uploader.actorType())
                || !Objects.equals(asset.getUploaderActorId(), uploader.actorId())
                || !Objects.equals(asset.getOrganizationId(), organizationId)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "不能跨用户、组织或项目关联产物文件");
        }
        if (asset.getBusinessScope() != FileBusinessScope.PRIVATE_DRAFT || asset.getProjectId() != null
                || asset.getStorageZone() != StorageZone.PRIVATE
                || !storageRepository.claimProjectArtifactDraft(
                asset, uploader.actorType(), uploader.actorId(), organizationId, projectId)) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "产物文件状态已变化，请重新选择");
        }
    }

    private AssetMetadata toMetadata(FileAssetEntity asset) {
        return new AssetMetadata(asset.getId(), asset.getUploaderActorType(), asset.getUploaderActorId(),
                asset.getOrganizationId(), asset.getProjectId(), asset.getBusinessScope(), asset.getVisibility(),
                asset.getStorageZone(), asset.getOriginalName(), asset.getMimeType(), asset.getFileExtension(), asset.getFileSize());
    }

    private StorageGateway activeGateway() {
        StorageProvider provider = storageProperties.getProvider();
        if (provider == StorageProvider.OSS && !storageProperties.getOss().isEnabled()) {
            throw new IllegalStateException("OSS storage provider requires app.storage.oss.enabled=true");
        }
        return gatewayFor(provider);
    }

    private StorageGateway gatewayFor(StorageProvider provider) {
        StorageProvider effective = provider == null ? StorageProvider.LOCAL : provider;
        StorageGateway gateway = gateways.get(effective);
        if (gateway == null) throw new IllegalStateException("Storage provider is not available: " + effective);
        return gateway;
    }

    private void validate(MultipartFile file, StorageScene scene) {
        if (file == null || file.isEmpty()) throw new ApiException(ApiErrorCode.BAD_REQUEST);
        if (file.getSize() > storageProperties.getMaxFileSize()) throw new ApiException(400, "文件大小超过限制");
        String safeName = safeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(safeName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new ApiException(400, "不支持的文件类型");
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
        String safeName = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (!StringUtils.hasText(safeName) || safeName.length() > 255 || ".".equals(safeName) || "..".equals(safeName)) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        return safeName;
    }

    private String extensionOf(String safeName) {
        int index = safeName.lastIndexOf('.');
        return index < 0 ? "" : safeName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeMimeType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT) : null;
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void compensateDelete(StorageGateway gateway, String bucketName, String objectKey, Exception original) {
        try {
            gateway.delete(bucketName, objectKey);
        } catch (Exception cleanupException) {
            original.addSuppressed(cleanupException);
        }
    }
}
