package com.advertisementdesign.back.communication.controller;

import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.common.storage.model.FileModels;
import com.advertisementdesign.back.communication.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Conversation", description = "会话与消息接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    @Operation(summary = "当前用户会话列表")
    @GetMapping("/conversations")
    public Result<List<ConversationModels.ConversationVO>> list() {
        return Result.success(conversationService.list());
    }

    @Operation(summary = "会话消息列表")
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<ConversationModels.MessageCursorPage> messages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(conversationService.messages(conversationId, beforeMessageId, size));
    }

    @Operation(summary = "上传会话私有附件")
    @PostMapping(value = "/conversations/{conversationId}/file-assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileModels.FileAssetVO> uploadAttachment(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "false") boolean image,
            @RequestPart("file") MultipartFile file) {
        return Result.success(conversationService.uploadAttachment(conversationId, image, file));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/conversations/{conversationId}/messages")
    public Result<ConversationModels.MessageVO> sendMessage(
            @PathVariable Long conversationId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ConversationModels.SendMessageRequest request) {
        return Result.success(conversationService.sendMessage(conversationId, request));
    }

    @Operation(summary = "标记会话已读")
    @PostMapping("/conversations/{conversationId}/read")
    public Result<ConversationModels.ConversationReadStateVO> markRead(
            @PathVariable Long conversationId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ConversationModels.MarkReadRequest request) {
        return Result.success(conversationService.markRead(conversationId, request));
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/messages/{messageId}")
    public Result<Boolean> deleteMessage(@PathVariable Long messageId) {
        return Result.success(conversationService.deleteMessage(messageId));
    }
}
