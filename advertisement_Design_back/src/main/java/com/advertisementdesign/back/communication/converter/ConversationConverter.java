package com.advertisementdesign.back.communication.converter;

import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.entity.ConversationReadStateEntity;
import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationConverter {
    private final ProjectRepository projectRepository;
    private final StorageRepository storageRepository;
    private final IdentityService identityService;

    public ConversationModels.ConversationVO toConversationVO(
            ConversationEntity entity,
            int unreadCount) {
        ProjectEntity project = entity.getProjectId() == null
                ? null
                : projectRepository.findProjectById(entity.getProjectId()).orElse(null);
        String customerName = identityService.findById(entity.getCustomerId())
                .map(IdentityService.UserProfile::nickname).orElse(null);
        String designerName = identityService.findById(entity.getDesignerId())
                .map(IdentityService.UserProfile::nickname).orElse(null);
        return new ConversationModels.ConversationVO(
                entity.getId(),
                entity.getProjectId(),
                project == null ? null : project.getName(),
                entity.getCustomerId(),
                customerName,
                entity.getDesignerId(),
                designerName,
                entity.getLastMessage(),
                entity.getLastMessageAt() == null ? null : entity.getLastMessageAt().toString(),
                unreadCount
        );
    }

    public ConversationModels.MessageVO toMessageVO(MessageEntity entity) {
        String senderName = entity.getSenderId() == null ? "系统" : identityService.findById(entity.getSenderId())
                .map(IdentityService.UserProfile::nickname)
                .orElse("系统");
        List<ConversationModels.FileAssetVO> files = entity.getFileIds() == null ? List.of() : entity.getFileIds().stream()
                .map(id -> storageRepository.findById(id).map(this::toFileVO).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ConversationModels.MessageVO(
                entity.getId(),
                entity.getConversationId(),
                entity.getSenderId(),
                entity.getSenderRole(),
                senderName,
                entity.getMessageType(),
                entity.getContent(),
                files,
                entity.getReplyToMessageId(),
                entity.getIsDeleted(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }

    public ConversationModels.ConversationReadStateVO toReadStateVO(
            ConversationReadStateEntity entity) {
        return new ConversationModels.ConversationReadStateVO(
                entity.getConversationId(),
                entity.getUserId(),
                entity.getLastReadMessageId(),
                entity.getLastReadAt() == null ? null : entity.getLastReadAt().toString(),
                entity.getUnreadCount()
        );
    }

    private ConversationModels.FileAssetVO toFileVO(FileAssetEntity entity) {
        return new ConversationModels.FileAssetVO(
                entity.getId(),
                entity.getOriginalName(),
                entity.getStorageName(),
                entity.getStorageProvider() == null ? null : entity.getStorageProvider().name(),
                entity.getBucketName(),
                entity.getObjectKey(),
                entity.getUrl(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getFileHash(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }
}
