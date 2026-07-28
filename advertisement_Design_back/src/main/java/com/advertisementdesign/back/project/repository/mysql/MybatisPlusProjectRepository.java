package com.advertisementdesign.back.project.repository.mysql;

import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectFileEntity;
import com.advertisementdesign.back.project.entity.ProjectStageEntity;
import com.advertisementdesign.back.project.entity.StageActionEntity;
import com.advertisementdesign.back.project.enums.FileRole;
import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.enums.StageActionStatus;
import com.advertisementdesign.back.project.mapper.ProjectFileMapper;
import com.advertisementdesign.back.project.mapper.ProjectMapper;
import com.advertisementdesign.back.project.mapper.ProjectStageMapper;
import com.advertisementdesign.back.project.mapper.StageActionMapper;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisPlusProjectRepository implements ProjectRepository {
    private static final int STAGE_COUNT = 7;

    private final ProjectMapper projectMapper;
    private final ProjectStageMapper projectStageMapper;
    private final StageActionMapper stageActionMapper;
    private final ProjectFileMapper projectFileMapper;

    @Override
    public List<ProjectEntity> listProjects() {
        return projectMapper.selectList(null);
    }

    @Override
    public Optional<ProjectEntity> findProjectById(Long id) {
        return Optional.ofNullable(projectMapper.selectById(id));
    }

    @Override
    public Optional<ProjectEntity> findProjectByConsultantIntakeId(Long consultantIntakeId) {
        return Optional.ofNullable(projectMapper.selectOne(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getConsultantIntakeId, consultantIntakeId)));
    }

    @Override
    public ProjectEntity saveProject(ProjectEntity project) {
        LocalDateTime now = LocalDateTime.now();
        if (project.getId() == null) {
            if (project.getCreatedAt() == null) {
                project.setCreatedAt(now);
            }
            project.setUpdatedAt(now);
            projectMapper.insert(project);
        } else {
            project.setUpdatedAt(now);
            projectMapper.updateById(project);
        }
        return project;
    }

    @Override
    public List<ProjectStageEntity> listStages(Long projectId) {
        return projectStageMapper.selectList(new LambdaQueryWrapper<ProjectStageEntity>()
                .eq(ProjectStageEntity::getProjectId, projectId)
                .orderByAsc(ProjectStageEntity::getSortOrder));
    }

    @Override
    public Optional<ProjectStageEntity> findStage(Long projectId, String stageCode) {
        return Optional.ofNullable(projectStageMapper.selectOne(new LambdaQueryWrapper<ProjectStageEntity>()
                .eq(ProjectStageEntity::getProjectId, projectId)
                .eq(ProjectStageEntity::getStageCode, stageCode)));
    }

    @Override
    public Optional<ProjectStageEntity> findStageById(Long id) {
        return Optional.ofNullable(projectStageMapper.selectById(id));
    }

    @Override
    public ProjectStageEntity saveStage(ProjectStageEntity stage) {
        stage.setUpdatedAt(LocalDateTime.now());
        if (stage.getId() == null) {
            projectStageMapper.insert(stage);
        } else {
            projectStageMapper.updateById(stage);
        }
        return stage;
    }

    @Override
    public StageActionEntity saveStageAction(StageActionEntity stageAction) {
        LocalDateTime now = LocalDateTime.now();
        if (stageAction.getId() == null) {
            if (stageAction.getCreatedAt() == null) {
                stageAction.setCreatedAt(now);
            }
            stageAction.setUpdatedAt(now);
            stageActionMapper.insert(stageAction);
        } else {
            stageAction.setUpdatedAt(now);
            stageActionMapper.updateById(stageAction);
        }
        return stageAction;
    }

    @Override
    public Optional<StageActionEntity> findStageActionById(Long id) {
        return Optional.ofNullable(stageActionMapper.selectById(id));
    }

    @Override
    public List<StageActionEntity> listStageActions(Long projectId, String stageCode, StageActionStatus status) {
        LambdaQueryWrapper<StageActionEntity> query = new LambdaQueryWrapper<StageActionEntity>()
                .eq(StageActionEntity::getProjectId, projectId)
                .orderByDesc(StageActionEntity::getRequestedAt);
        if (stageCode != null && !stageCode.isBlank()) {
            query.eq(StageActionEntity::getStageCode, stageCode);
        }
        if (status != null) {
            query.eq(StageActionEntity::getStatus, status);
        }
        return stageActionMapper.selectList(query);
    }

    @Override
    public List<ProjectFileEntity> listProjectFiles(Long projectId, String stageCode, FileRole fileRole) {
        LambdaQueryWrapper<ProjectFileEntity> query = new LambdaQueryWrapper<ProjectFileEntity>()
                .eq(ProjectFileEntity::getProjectId, projectId)
                .orderByDesc(ProjectFileEntity::getCreatedAt);
        if (stageCode != null && !stageCode.isBlank()) {
            query.eq(ProjectFileEntity::getStageCode, stageCode);
        }
        if (fileRole != null) {
            query.eq(ProjectFileEntity::getFileRole, fileRole);
        }
        return projectFileMapper.selectList(query);
    }

    @Override
    public ProjectFileEntity saveProjectFile(ProjectFileEntity projectFile) {
        if (projectFile.getId() == null) {
            if (projectFile.getCreatedAt() == null) {
                projectFile.setCreatedAt(LocalDateTime.now());
            }
            projectFileMapper.insert(projectFile);
        } else {
            projectFileMapper.updateById(projectFile);
        }
        return projectFile;
    }

    @Override
    public Optional<ProjectFileEntity> findProjectFileById(Long id) {
        return Optional.ofNullable(projectFileMapper.selectById(id));
    }

    @Override
    public boolean existsProjectFile(Long projectId, Long fileId) {
        return projectFileMapper.selectCount(
                new LambdaQueryWrapper<ProjectFileEntity>()
                        .eq(ProjectFileEntity::getProjectId, projectId)
                        .eq(ProjectFileEntity::getFileId, fileId)) > 0;
    }

    @Override
    public boolean canUserAccessFile(Long fileId, Long userId) {
        return projectFileMapper.countAccessibleByUser(fileId, userId) > 0;
    }

    @Override
    public boolean deleteProjectFile(Long id) {
        return projectFileMapper.deleteById(id) > 0;
    }

    @Override
    public long countInProgressProjectsByDesigner(Long designerId) {
        return projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getDesignerId, designerId)
                .eq(ProjectEntity::getStatus, ProjectStatus.IN_PROGRESS));
    }

    @Override
    public void refreshProjectProgress(Long projectId) {
        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        long reached = projectStageMapper.selectCount(new LambdaQueryWrapper<ProjectStageEntity>()
                .eq(ProjectStageEntity::getProjectId, projectId)
                .eq(ProjectStageEntity::getStatus, ProjectStageStatus.REACHED));
        project.setProgress((int) Math.round(reached * 100.0 / STAGE_COUNT));
        if (reached >= STAGE_COUNT) {
            project.setStatus(ProjectStatus.COMPLETED);
        }
        saveProject(project);
    }
}
