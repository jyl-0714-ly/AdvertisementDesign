package com.advertisementdesign.back.project.repository;

import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectFileEntity;
import com.advertisementdesign.back.project.entity.ProjectStageEntity;
import com.advertisementdesign.back.project.entity.StageActionEntity;
import com.advertisementdesign.back.project.enums.FileRole;
import com.advertisementdesign.back.project.enums.StageActionStatus;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    List<ProjectEntity> listProjects();

    Optional<ProjectEntity> findProjectById(Long id);

    ProjectEntity saveProject(ProjectEntity project);

    List<ProjectStageEntity> listStages(Long projectId);

    Optional<ProjectStageEntity> findStage(Long projectId, String stageCode);

    Optional<ProjectStageEntity> findStageById(Long id);

    ProjectStageEntity saveStage(ProjectStageEntity stage);

    StageActionEntity saveStageAction(StageActionEntity stageAction);

    Optional<StageActionEntity> findStageActionById(Long id);

    List<StageActionEntity> listStageActions(
            Long projectId,
            String stageCode,
            StageActionStatus status
    );

    List<ProjectFileEntity> listProjectFiles(
            Long projectId,
            String stageCode,
            FileRole fileRole
    );

    ProjectFileEntity saveProjectFile(ProjectFileEntity projectFile);

    Optional<ProjectFileEntity> findProjectFileById(Long id);

    boolean existsProjectFile(Long projectId, Long fileId);

    boolean isFileAssociatedWithProject(Long fileId);

    boolean canUserAccessFile(Long fileId, Long userId);

    boolean deleteProjectFile(Long id);

    long countInProgressProjectsByDesigner(Long designerId);

    void refreshProjectProgress(Long projectId);
}
