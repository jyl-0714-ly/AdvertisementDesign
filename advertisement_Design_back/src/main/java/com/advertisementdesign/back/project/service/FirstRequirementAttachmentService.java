package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.service.FileService;
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
            "application/pdf", "text/plain", "text/csv", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed", "application/octet-stream");

    private final FileService fileService;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> validateAndClaim(Command command) {
        if (command.actor().type() != ActorRef.ActorType.CUSTOMER_USER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "仅客户可以提交首条项目需求");
        }
        List<Long> ids = command.fileAssetIds() == null ? List.of()
                : List.copyOf(new LinkedHashSet<>(command.fileAssetIds()));
        if (ids.size() > MAX_FILES) throw invalid("附件数量超过限制");
        long totalSize = 0L;
        for (Long id : ids) {
            FileService.AssetMetadata asset = fileService.requireActiveMetadata(id);
            if (!command.actor().type().name().equals(asset.uploaderActorType())
                    || !command.actor().actorId().equals(asset.uploaderActorId())) {
                throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "不能关联其他客户上传的附件");
            }
            if (asset.organizationId() != null && !command.organizationId().equals(asset.organizationId())) {
                throw new ApiException(ApiErrorCode.FORBIDDEN.getCode(), "不能跨组织关联附件");
            }
            if (asset.businessScope() != FileBusinessScope.PRIVATE_DRAFT || asset.projectId() != null) {
                throw invalid("附件已不可用或已被其他项目使用");
            }
            long size = asset.fileSize() == null ? 0 : asset.fileSize();
            if (size <= 0 || size > MAX_SINGLE_SIZE) throw invalid("附件大小不符合限制");
            totalSize += size;
            String extension = asset.fileExtension() == null ? "" : asset.fileExtension().toLowerCase(Locale.ROOT);
            String mime = asset.mimeType() == null ? "" : asset.mimeType().toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_MIME_TYPES.contains(mime)) {
                throw invalid("附件类型不受支持");
            }
        }
        if (totalSize > MAX_TOTAL_SIZE) throw invalid("附件总大小超过限制");
        FileService.Uploader uploader = new FileService.Uploader(command.actor().type().name(), command.actor().actorId());
        ids.forEach(id -> fileService.claimFirstRequirementDraft(id, uploader, command.organizationId(), command.projectId()));
        return ids;
    }

    private ApiException invalid(String message) {
        return new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), message);
    }

    public record Command(ActorRef actor, Long organizationId, Long projectId, List<Long> fileAssetIds) {}
}
