package com.advertisementdesign.back.api;

import com.advertisementdesign.back.api.auth.AuthModels;
import com.advertisementdesign.back.api.conversation.ConversationModels;
import com.advertisementdesign.back.api.file.FileModels;
import com.advertisementdesign.back.api.operation.OperationLogModels;
import com.advertisementdesign.back.api.portfolio.PortfolioModels;
import com.advertisementdesign.back.api.project.ProjectModels;
import com.advertisementdesign.back.api.stage.StageModels;
import com.advertisementdesign.back.domain.entity.*;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiAssembler {
    private final DemoDataStore store;

    public AuthModels.UserVO toUserVO(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AuthModels.UserVO(entity.getId(), entity.getEmail(), entity.getNickname(), entity.getRole(), entity.getAvatar());
    }

    public AuthModels.LoginResponse toLoginResponse(String token, UserEntity user) {
        return new AuthModels.LoginResponse(token, toUserVO(user));
    }

    public PortfolioModels.PortfolioCaseVO toPortfolioCaseVO(PortfolioCaseEntity entity) {
        return new PortfolioModels.PortfolioCaseVO(
                entity.getId(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getIndustry(),
                entity.getStyle(),
                entity.getServiceType(),
                entity.getCoverUrl(),
                entity.getImageUrls(),
                entity.getDescription(),
                entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getFeatured()),
                entity.getStatus(),
                entity.getCreatedAt().toString(),
                entity.getUpdatedAt().toString()
        );
    }

    public ProjectModels.ProjectVO toProjectVO(ProjectEntity entity) {
        UserEntity customer = store.findUserById(entity.getCustomerId()).orElse(null);
        UserEntity designer = store.findUserById(entity.getDesignerId()).orElse(null);
        String stageName = store.findStages(entity.getId()).stream()
                .filter(stage -> stage.getStageCode().equals(entity.getCurrentStage()))
                .findFirst()
                .map(ProjectStageEntity::getStageName)
                .orElse(entity.getCurrentStage());
        return new ProjectModels.ProjectVO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCustomerId(),
                customer == null ? null : customer.getNickname(),
                entity.getDesignerId(),
                designer == null ? null : designer.getNickname(),
                entity.getCurrentStage(),
                stageName,
                entity.getStatus(),
                entity.getProgress(),
                entity.getCreatedAt().toString(),
                entity.getUpdatedAt().toString()
        );
    }

    public ProjectModels.ProjectStageVO toProjectStageVO(ProjectStageEntity entity) {
        return new ProjectModels.ProjectStageVO(
                entity.getId(),
                entity.getProjectId(),
                entity.getStageCode(),
                entity.getStageName(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getReachedAt() == null ? null : entity.getReachedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }

    public StageModels.StageActionVO toStageActionVO(StageActionEntity entity) {
        return new StageModels.StageActionVO(
                entity.getId(),
                entity.getProjectId(),
                entity.getProjectStageId(),
                entity.getStageCode(),
                entity.getInitiatorId(),
                entity.getInitiatorRole(),
                entity.getConfirmUserId(),
                entity.getStatus(),
                entity.getRequestNote(),
                entity.getResponseNote(),
                entity.getRequestedAt() == null ? null : entity.getRequestedAt().toString(),
                entity.getRespondedAt() == null ? null : entity.getRespondedAt().toString(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }

    public ConversationModels.ConversationVO toConversationVO(ConversationEntity entity) {
        return toConversationVO(entity, Math.max(store.unreadCount(entity.getId(), entity.getCustomerId()), store.unreadCount(entity.getId(), entity.getDesignerId())));
    }

    public ConversationModels.ConversationVO toConversationVO(ConversationEntity entity, int unreadCount) {
        ProjectEntity project = store.findProjectById(entity.getProjectId()).orElse(null);
        UserEntity customer = store.findUserById(entity.getCustomerId()).orElse(null);
        UserEntity designer = store.findUserById(entity.getDesignerId()).orElse(null);
        return new ConversationModels.ConversationVO(
                entity.getId(),
                entity.getProjectId(),
                project == null ? null : project.getName(),
                entity.getCustomerId(),
                customer == null ? null : customer.getNickname(),
                entity.getDesignerId(),
                designer == null ? null : designer.getNickname(),
                entity.getLastMessage(),
                entity.getLastMessageAt() == null ? null : entity.getLastMessageAt().toString(),
                unreadCount
        );
    }

    public ConversationModels.MessageVO toMessageVO(MessageEntity entity) {
        UserEntity sender = entity.getSenderId() == null ? null : store.findUserById(entity.getSenderId()).orElse(null);
        List<ConversationModels.FileAssetVO> files = entity.getFileIds() == null ? List.of() : entity.getFileIds().stream()
                .map(id -> store.findFileAssetById(id).map(this::toConversationFileVO).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ConversationModels.MessageVO(
                entity.getId(),
                entity.getConversationId(),
                entity.getSenderId(),
                entity.getSenderRole(),
                sender == null ? "系统" : sender.getNickname(),
                entity.getMessageType(),
                entity.getContent(),
                files,
                entity.getReplyToMessageId(),
                entity.getIsDeleted(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }

    public ConversationModels.FileAssetVO toConversationFileVO(FileAssetEntity entity) {
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

    public FileModels.FileAssetVO toFileVO(FileAssetEntity entity) {
        return new FileModels.FileAssetVO(
                entity.getId(),
                entity.getOriginalName(),
                entity.getStorageName(),
                entity.getStorageProvider(),
                entity.getBucketName(),
                entity.getObjectKey(),
                entity.getUrl(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getFileHash(),
                entity.getStatus(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }

    public FileModels.ProjectFileVO toProjectFileVO(ProjectFileEntity entity) {
        FileAssetEntity file = store.findFileAssetById(entity.getFileId()).orElse(null);
        return new FileModels.ProjectFileVO(
                entity.getId(),
                entity.getProjectId(),
                entity.getProjectStageId(),
                entity.getStageCode(),
                entity.getFileId(),
                entity.getUploaderId(),
                entity.getFileRole(),
                entity.getDescription(),
                file == null ? null : toFileVO(file),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString()
        );
    }

    public ConversationModels.ConversationReadStateVO toReadStateVO(ConversationReadStateEntity entity) {
        return new ConversationModels.ConversationReadStateVO(
                entity.getConversationId(),
                entity.getUserId(),
                entity.getLastReadMessageId(),
                entity.getLastReadAt() == null ? null : entity.getLastReadAt().toString(),
                entity.getUnreadCount()
        );
    }

    public OperationLogModels.OperationLogVO toOperationLogVO(OperationLogEntity entity) {
        return new OperationLogModels.OperationLogVO(
                entity.getId(),
                entity.getOperatorId(),
                entity.getOperatorRole(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getAction(),
                entity.getDescription(),
                entity.getBeforeData(),
                entity.getAfterData(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString()
        );
    }
}
