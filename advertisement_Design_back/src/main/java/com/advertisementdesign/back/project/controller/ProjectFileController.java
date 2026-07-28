package com.advertisementdesign.back.project.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.project.enums.FileRole;
import com.advertisementdesign.back.project.model.ProjectFileModels;
import com.advertisementdesign.back.project.service.ProjectFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
import com.advertisementdesign.back.common.storage.model.FileModels;

import java.util.List;

@Tag(name = "Project File", description = "项目文件接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectFileController {
    private final ProjectFileService projectFileService;

    @Operation(summary = "项目文件列表")
    @GetMapping("/projects/{projectId}/files")
    public Result<List<ProjectFileModels.ProjectFileVO>> listProjectFiles(
            @PathVariable Long projectId,
            @RequestParam(required = false) String stageCode,
            @RequestParam(required = false) FileRole fileRole) {
        return Result.success(projectFileService.listProjectFiles(projectId, stageCode, fileRole));
    }

    @Operation(summary = "上传项目私有文件")
    @PostMapping(value = "/projects/{projectId}/file-assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileModels.FileAssetVO> uploadProjectFile(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "DELIVERABLE") FileRole fileRole,
            @RequestPart("file") MultipartFile file) {
        return Result.success(projectFileService.uploadProjectFile(projectId, fileRole, file));
    }

    @Operation(summary = "归档项目文件")
    @PostMapping("/projects/{projectId}/files")
    public Result<ProjectFileModels.ProjectFileVO> archiveProjectFile(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectFileModels.CreateProjectFileRequest request) {
        return Result.success(projectFileService.archiveProjectFile(projectId, request));
    }

    @Operation(summary = "删除项目文件归档")
    @DeleteMapping("/project-files/{projectFileId}")
    public Result<Boolean> deleteProjectFile(@PathVariable Long projectFileId) {
        return Result.success(projectFileService.deleteProjectFile(projectFileId));
    }
}
