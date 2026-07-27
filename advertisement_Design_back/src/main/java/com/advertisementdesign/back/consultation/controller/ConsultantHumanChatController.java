package com.advertisementdesign.back.consultation.controller;

import com.advertisementdesign.back.consultation.model.ConsultantHumanChatModels;
import com.advertisementdesign.back.common.api.Result;
import com.advertisementdesign.back.consultation.service.ConsultantHumanChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ConsultantHumanChat", description = "顾问需求交接后的专属人工服务会话接口")
@RestController
@RequestMapping("/api/consultant-intakes/human-chats")
@RequiredArgsConstructor
public class ConsultantHumanChatController {
    private final ConsultantHumanChatService consultantHumanChatService;

    @Operation(summary = "获取人工服务会话消息")
    @GetMapping("/{humanChatId}/messages")
    public Result<List<ConsultantHumanChatModels.HumanMessageVO>> listMessages(
            @PathVariable String humanChatId) {
        return Result.success(consultantHumanChatService.listMessages(humanChatId));
    }

    @Operation(summary = "发送人工服务会话消息")
    @PostMapping("/{humanChatId}/messages")
    public Result<ConsultantHumanChatModels.HumanMessageVO> sendMessage(
            @PathVariable String humanChatId,
            @Valid @RequestBody ConsultantHumanChatModels.SendHumanMessageRequest request) {
        return Result.success(consultantHumanChatService.sendMessage(humanChatId, request));
    }
}
