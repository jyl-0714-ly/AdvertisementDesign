package com.advertisementdesign.back.controller;

import com.advertisementdesign.back.api.file.FileModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.domain.enums.FileRole;
import com.advertisementdesign.back.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "File", description = "文件接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileModels.FileAssetVO> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }

    @Operation(summary = "文件详情")
    @GetMapping("/files/{fileId}")
    public Result<FileModels.FileAssetVO> detail(@PathVariable Long fileId) {
        return Result.success(fileService.detail(fileId));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
        FileModels.FileAssetVO file = fileService.detail(fileId);
        byte[] body = fileService.download(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.originalName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(body);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/files/{fileId}")
    public Result<Boolean> delete(@PathVariable Long fileId) {
        return Result.success(fileService.delete(fileId));
    }

    @Operation(summary = "项目文件列表")
    @GetMapping("/projects/{projectId}/files")
    public Result<List<FileModels.ProjectFileVO>> listProjectFiles(
            @PathVariable Long projectId,
            @RequestParam(required = false) String stageCode,
            @RequestParam(required = false) FileRole fileRole) {
        return Result.success(fileService.listProjectFiles(projectId, stageCode, fileRole));
    }

    @Operation(summary = "归档项目文件")
    @PostMapping("/projects/{projectId}/files")
    public Result<FileModels.ProjectFileVO> archiveProjectFile(
            @PathVariable Long projectId,
            @Valid @RequestBody FileModels.CreateProjectFileRequest request) {
        return Result.success(fileService.archiveProjectFile(projectId, request));
    }

    @Operation(summary = "删除项目文件归档")
    @DeleteMapping("/project-files/{projectFileId}")
    public Result<Boolean> deleteProjectFile(@PathVariable Long projectFileId) {
        return Result.success(fileService.deleteProjectFile(projectFileId));
    }
}
