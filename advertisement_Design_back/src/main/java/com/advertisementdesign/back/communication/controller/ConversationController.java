package com.advertisementdesign.back.communication.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.communication.service.CommunicationFileService;
import com.advertisementdesign.back.communication.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "Project Conversation", description = "项目上下文中的会话与消息查询")
@RestController
@RequestMapping("/api/projects/{projectId}/conversation")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final CommunicationFileService communicationFileService;

    @Operation(summary = "查询项目会话")
    @GetMapping
    public Result<ConversationModels.ConversationView> conversation(@PathVariable Long projectId) {
        return Result.success(conversationService.conversation(projectId));
    }

    @Operation(summary = "查询项目会话消息")
    @GetMapping("/messages")
    public Result<ConversationModels.MessagePage> messages(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(conversationService.customerMessages(projectId, beforeMessageId, size));
    }

    @Operation(summary = "上传项目消息草稿附件")
    @PostMapping(value = "/message-drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<com.advertisementdesign.back.common.storage.model.FileModels.CustomerSafeFileMetadata> uploadDraft(
            @PathVariable Long projectId, @RequestPart("file") MultipartFile file) {
        return Result.success(communicationFileService.uploadDraft(projectId, file));
    }

    @Operation(summary = "流式下载项目消息附件")
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long projectId, @PathVariable Long fileId) {
        CommunicationFileService.Download download = communicationFileService.openDownload(projectId, fileId);
        return ResponseEntity.ok()
                .contentLength(download.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.name(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }

    @Operation(summary = "发送项目会话消息")
    @PostMapping("/messages")
    public Result<ConversationModels.CustomerMessageView> sendMessage(
            @PathVariable Long projectId,
            @RequestBody ConversationModels.SendMessageRequest request) {
        return Result.success(conversationService.appendAsCurrentUser(
                new ConversationModels.CurrentUserAppendCommand(
                        projectId, request.content(), request.replyToMessageId(),
                        request.correctionMessageId(), request.clientMessageId(), request.fileAssetIds())));
    }
}
