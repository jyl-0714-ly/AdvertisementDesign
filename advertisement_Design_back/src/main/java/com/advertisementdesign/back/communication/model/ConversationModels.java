package com.advertisementdesign.back.communication.model;

import com.advertisementdesign.back.communication.enums.ConversationStatus;
import com.advertisementdesign.back.communication.enums.ConversationType;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.communication.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "会话相关模型")
public final class ConversationModels {
    private ConversationModels() {
    }

    @Schema(description = "会话视图")
    public record ConversationVO(
            Long id,
            Long projectId,
            String projectName,
            ConversationType conversationType,
            ConversationStatus status,
            Long customerId,
            String customerName,
            Long designerId,
            String designerName,
            String lastMessage,
            String lastMessageAt,
            Integer unreadCount
    ) {
    }

    @Schema(description = "发送消息请求")
    public record SendMessageRequest(
            @NotNull MessageType messageType,
            String content,
            List<Long> fileIds,
            String clientMessageId
    ) {
    }

    @Schema(description = "消息视图")
    public record MessageVO(
            Long id,
            Long conversationId,
            Long senderId,
            MessageSenderRole senderRole,
            String senderName,
            MessageType messageType,
            String content,
            List<FileAssetVO> files,
            Long replyToMessageId,
            Boolean isDeleted,
            String createdAt,
            String updatedAt
    ) {
    }

    @Schema(description = "消息中的文件视图")
    public record FileAssetVO(
            Long id,
            String originalName,
            String storageName,
            String storageProvider,
            String bucketName,
            String objectKey,
            String url,
            String mimeType,
            Long fileSize,
            String fileHash,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    @Schema(description = "会话已读状态视图")
    public record ConversationReadStateVO(
            Long conversationId,
            Long userId,
            Long lastReadMessageId,
            String lastReadAt,
            Integer unreadCount
    ) {
    }

    @Schema(description = "标记已读请求")
    public record MarkReadRequest(@NotNull Long lastReadMessageId) {
    }

    @Schema(description = "消息游标分页")
    public record MessageCursorPage(List<MessageVO> records, Boolean hasMore) {
    }
}
