package com.advertisementdesign.back.project.converter;

import com.advertisementdesign.back.common.storage.converter.FileConverter;
import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.repository.StorageRepository;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectFileEntity;
import com.advertisementdesign.back.project.entity.ProjectStageEntity;
import com.advertisementdesign.back.project.entity.StageActionEntity;
import com.advertisementdesign.back.project.model.ProjectFileModels;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.model.StageModels;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectConverter {
    private final ProjectRepository projectRepository;
    private final StorageRepository storageRepository;
    private final IdentityService identityService;
    private final FileConverter fileConverter;

    public ProjectModels.ProjectVO toProjectVO(ProjectEntity entity) {
        String customerName = identityService.findById(entity.getCustomerId())
                .map(IdentityService.UserProfile::nickname).orElse(null);
        String designerName = identityService.findById(entity.getDesignerId())
                .map(IdentityService.UserProfile::nickname).orElse(null);
        String stageName = projectRepository.listStages(entity.getId()).stream()
                .filter(stage -> stage.getStageCode().equals(entity.getCurrentStage()))
                .findFirst()
                .map(ProjectStageEntity::getStageName)
                .orElse(entity.getCurrentStage());
        return new ProjectModels.ProjectVO(
                entity.getId(), entity.getName(), entity.getDescription(),
                entity.getCustomerId(), customerName,
                entity.getDesignerId(), designerName,
                entity.getCurrentStage(), stageName, entity.getStatus(), entity.getProgress(),
                entity.getCreatedAt().toString(), entity.getUpdatedAt().toString()
        );
    }

    public ProjectModels.ProjectStageVO toProjectStageVO(ProjectStageEntity entity) {
        return new ProjectModels.ProjectStageVO(
                entity.getId(), entity.getProjectId(), entity.getStageCode(), entity.getStageName(),
                entity.getSortOrder(), entity.getStatus(),
                entity.getReachedAt() == null ? null : entity.getReachedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }

    public StageModels.StageActionVO toStageActionVO(StageActionEntity entity) {
        return new StageModels.StageActionVO(
                entity.getId(), entity.getProjectId(), entity.getProjectStageId(), entity.getStageCode(),
                entity.getInitiatorId(), entity.getInitiatorRole(), entity.getConfirmUserId(), entity.getStatus(),
                entity.getRequestNote(), entity.getResponseNote(),
                entity.getRequestedAt() == null ? null : entity.getRequestedAt().toString(),
                entity.getRespondedAt() == null ? null : entity.getRespondedAt().toString(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }

    public ProjectFileModels.ProjectFileVO toProjectFileVO(ProjectFileEntity entity) {
        FileAssetEntity file = storageRepository.findById(entity.getFileId()).orElse(null);
        return new ProjectFileModels.ProjectFileVO(
                entity.getId(), entity.getProjectId(), entity.getProjectStageId(), entity.getStageCode(),
                entity.getFileId(), entity.getUploaderId(), entity.getFileRole(), entity.getDescription(),
                file == null ? null : fileConverter.toVO(file),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString()
        );
    }
}
