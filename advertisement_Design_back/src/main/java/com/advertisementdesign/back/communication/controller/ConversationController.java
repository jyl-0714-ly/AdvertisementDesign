package com.advertisementdesign.back.communication.controller;

import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.communication.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Project Conversation", description = "项目上下文中的会话与消息查询")
@RestController
@RequestMapping("/api/projects/{projectId}/conversation")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    @Operation(summary = "查询项目会话")
    @GetMapping
    public Result<ConversationModels.ConversationView> conversation(@PathVariable Long projectId) {
        return Result.success(conversationService.conversation(projectId));
    }

    @Operation(summary = "查询项目会话消息")
    @GetMapping("/messages")
    public Result<List<ConversationModels.CustomerMessageView>> messages(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(conversationService.customerMessages(projectId, beforeMessageId, size));
    }
}
