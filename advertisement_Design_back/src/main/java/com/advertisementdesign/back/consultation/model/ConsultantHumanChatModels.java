package com.advertisementdesign.back.consultation.model;

import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "顾问交接人工服务会话模型")
public final class ConsultantHumanChatModels {
    private ConsultantHumanChatModels() {
    }

    @Schema(description = "发送人工服务消息请求")
    public record SendHumanMessageRequest(
            @NotBlank(message = "消息内容不能为空")
            @Size(max = 2000, message = "消息内容不能超过 2000 个字符")
            String content
    ) {
    }

    @Schema(description = "人工服务消息")
    public record HumanMessageVO(
            Long id,
            String humanChatId,
            Long senderId,
            MessageSenderRole senderRole,
            String senderName,
            String content,
            String createdAt
    ) {
    }
}
