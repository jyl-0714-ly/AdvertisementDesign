package com.advertisementdesign.back.common.storage.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.identity.model.ActorRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FirstRequirementAttachmentService {
    private static final int MAX_FILES = 10;
    private static final long MAX_SINGLE_SIZE = 25L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 80L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "txt", "csv",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            "application/pdf", "text/plain", "text/csv",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            "application/octet-stream");

    private final StorageRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> validateAndClaim(Command command) {
        if (command.actor().type() != ActorRef.ActorType.CUSTOMER_USER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "仅客户可以提交首条项目需求");
        }
        List<Long> ids = command.fileAssetIds() == null ? List.of() : List.copyOf(new LinkedHashSet<>(command.fileAssetIds()));
        if (ids.size() > MAX_FILES) {
            throw invalidAttachment("附件数量超过限制");
        }
        long totalSize = 0L;
        List<FileAssetEntity> assets = ids.stream().map(id -> repository.findById(id)
                .orElseThrow(() -> invalidAttachment("附件不存在或不可用"))).toList();
        for (FileAssetEntity asset : assets) {
            if (asset.getUploaderActorType() != ActorRef.ActorType.CUSTOMER_USER
                    || !command.actor().actorId().equals(asset.getUploaderActorId())) {
                throw forbiddenAttachment("不能关联其他客户上传的附件");
            }
            if (asset.getOrganizationId() != null
                    && !command.organizationId().equals(asset.getOrganizationId())) {
                throw forbiddenAttachment("不能跨组织关联附件");
            }
            if (asset.getStatus() != FileStatus.ACTIVE
                    || asset.getBusinessScope() != FileBusinessScope.PRIVATE_DRAFT
                    || asset.getProjectId() != null) {
                throw invalidAttachment("附件已不可用或已被其他项目使用");
            }
            long size = asset.getFileSize() == null ? 0L : asset.getFileSize();
            if (size <= 0 || size > MAX_SINGLE_SIZE) {
                throw invalidAttachment("附件大小不符合限制");
            }
            totalSize += size;
            String extension = asset.getFileExtension() == null ? "" : asset.getFileExtension().toLowerCase(Locale.ROOT);
            String mimeType = asset.getMimeType() == null ? "" : asset.getMimeType().toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw invalidAttachment("附件类型不受支持");
            }
        }
        if (totalSize > MAX_TOTAL_SIZE) {
            throw invalidAttachment("附件总大小超过限制");
        }
        for (FileAssetEntity asset : assets) {
            if (!repository.claimFirstRequirementDraft(
                    asset, command.actor().actorId(), command.organizationId(), command.projectId())) {
                throw invalidAttachment("附件状态已变化，请重新选择");
            }
        }
        return ids;
    }

    private ApiException invalidAttachment(String message) {
        return new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), message);
    }

    private ApiException forbiddenAttachment(String message) {
        return new ApiException(ApiErrorCode.FORBIDDEN.getCode(), message);
    }

    public record Command(ActorRef actor, Long organizationId, Long projectId, List<Long> fileAssetIds) {
    }
}
