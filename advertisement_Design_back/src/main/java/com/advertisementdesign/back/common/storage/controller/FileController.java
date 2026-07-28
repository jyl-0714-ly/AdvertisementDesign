package com.advertisementdesign.back.common.storage.controller;

import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.common.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Tag(name = "File", description = "文件接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @Deprecated(forRemoval = true)
    @Operation(
            summary = "上传通用私有文件（兼容接口）",
            deprecated = true,
            description = "仅为旧客户端保留；新业务必须使用作品、项目或会话上下文中的上传接口")
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
}
