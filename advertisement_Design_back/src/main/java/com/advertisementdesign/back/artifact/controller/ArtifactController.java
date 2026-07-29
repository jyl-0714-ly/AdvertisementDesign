package com.advertisementdesign.back.artifact.controller;

import com.advertisementdesign.back.artifact.model.ArtifactModels;
import com.advertisementdesign.back.artifact.service.ArtifactService;
import com.advertisementdesign.back.artifact.service.ArtifactFileService;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.common.storage.model.FileModels;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Project Artifacts", description = "项目产物、不可变版本、审核和确认")
@RestController
@RequestMapping("/api/projects/{projectId}/artifacts")
@RequiredArgsConstructor
public class ArtifactController {
    private final ArtifactService artifactService;
    private final ArtifactFileService artifactFileService;

    @Operation(summary = "创建项目产物")
    @PostMapping
    public Result<ArtifactModels.ArtifactView> create(@PathVariable Long projectId,
                                                       @Valid @RequestBody ArtifactModels.CreateArtifactRequest request) {
        return Result.success(artifactService.create(projectId, request));
    }

    @Operation(summary = "创建产物新版本")
    @PostMapping("/{artifactId}/versions")
    public Result<ArtifactModels.VersionView> createVersion(@PathVariable Long projectId, @PathVariable Long artifactId,
                                                             @Valid @RequestBody ArtifactModels.CreateVersionRequest request) {
        return Result.success(artifactService.createVersion(projectId, artifactId, request));
    }

    @Operation(summary = "修改未发布草稿版本")
    @PutMapping("/{artifactId}/versions/{versionId}")
    public Result<ArtifactModels.VersionView> updateDraft(@PathVariable Long projectId, @PathVariable Long artifactId,
                                                           @PathVariable Long versionId,
                                                           @Valid @RequestBody ArtifactModels.UpdateDraftRequest request) {
        return Result.success(artifactService.updateDraft(projectId, artifactId, versionId, request));
    }

    @Operation(summary = "审核产物版本")
    @PostMapping("/{artifactId}/versions/{versionId}/approval")
    public Result<ArtifactModels.VersionView> approve(@PathVariable Long projectId, @PathVariable Long artifactId,
                                                       @PathVariable Long versionId,
                                                       @Valid @RequestBody ArtifactModels.ApproveRequest request) {
        return Result.success(artifactService.approve(projectId, artifactId, versionId, request));
    }

    @Operation(summary = "发布产物版本")
    @PostMapping("/{artifactId}/versions/{versionId}/publication")
    public Result<ArtifactModels.VersionView> publish(@PathVariable Long projectId, @PathVariable Long artifactId,
                                                       @PathVariable Long versionId,
                                                       @Valid @RequestBody ArtifactModels.PublishRequest request) {
        return Result.success(artifactService.publish(projectId, artifactId, versionId, request));
    }

    @Operation(summary = "客户确认指定不可变版本")
    @PostMapping("/{artifactId}/versions/{versionId}/confirmations")
    public Result<ArtifactModels.ConfirmationView> confirm(@PathVariable Long projectId, @PathVariable Long artifactId,
                                                            @PathVariable Long versionId,
                                                            @Valid @RequestBody ArtifactModels.ConfirmRequest request) {
        return Result.success(artifactService.confirm(projectId, artifactId, versionId, request));
    }

    @Operation(summary = "上传私有产物文件草稿")
    @PostMapping(value = "/file-drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileModels.FileAssetVO> uploadFileDraft(@PathVariable Long projectId,
                                                          @RequestPart("file") MultipartFile file) {
        return Result.success(artifactFileService.uploadDraft(projectId, file));
    }

    @Operation(summary = "关联文件到产物版本")
    @PostMapping("/{artifactId}/versions/{versionId}/files")
    public Result<Boolean> attachFile(@PathVariable Long projectId, @PathVariable Long artifactId,
                                      @PathVariable Long versionId,
                                      @Valid @RequestBody ArtifactModels.AttachFileRequest request) {
        artifactFileService.attach(projectId, artifactId, versionId, request.fileId(),
                request.fileRole(), request.displayOrder());
        return Result.success(true);
    }

    @Operation(summary = "下载私有产物版本文件")
    @GetMapping("/{artifactId}/versions/{versionId}/files/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long projectId,
                                                             @PathVariable Long artifactId,
                                                             @PathVariable Long versionId,
                                                             @PathVariable Long fileId) {
        ArtifactFileService.Download download = artifactFileService.openDownload(projectId, artifactId, versionId, fileId);
        return ResponseEntity.ok().contentLength(download.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.name(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }

    @Operation(summary = "标注未发布产物版本文件")
    @PostMapping("/{artifactId}/versions/{versionId}/annotations")
    public Result<ArtifactModels.AnnotationView> annotate(@PathVariable Long projectId,
                                                          @PathVariable Long artifactId,
                                                          @PathVariable Long versionId,
                                                          @Valid @RequestBody ArtifactModels.AnnotationRequest request) {
        return Result.success(artifactService.annotate(projectId, artifactId, versionId, request));
    }

    @Operation(summary = "查询客户可见产物版本")
    @GetMapping("/{artifactId}/versions/{versionId}")
    public Result<ArtifactModels.VersionView> customerVersion(@PathVariable Long projectId, @PathVariable Long artifactId,
                                                               @PathVariable Long versionId) {
        return Result.success(artifactService.customerVersion(projectId, artifactId, versionId));
    }
}
