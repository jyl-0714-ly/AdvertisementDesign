package com.advertisementdesign.back.project.repository.mysql;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.ProjectNameSource;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.mapper.ProjectMapper;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisPlusProjectRepository implements ProjectRepository {
    private final ProjectMapper projectMapper;

    @Override
    public List<ProjectEntity> findAll(ProjectStatus status, String keyword) {
        LambdaQueryWrapper<ProjectEntity> query = new LambdaQueryWrapper<ProjectEntity>()
                .orderByDesc(ProjectEntity::getUpdatedAt);
        if (status != null) {
            query.eq(ProjectEntity::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like(ProjectEntity::getName, keyword.trim());
        }
        return projectMapper.selectList(query);
    }

    @Override
    public Optional<ProjectEntity> findById(Long id) {
        return Optional.ofNullable(projectMapper.selectById(id));
    }

    @Override
    public ProjectEntity save(ProjectEntity project) {
        LocalDateTime now = LocalDateTime.now();
        if (project.getId() == null) {
            if (project.getName() == null || project.getName().isBlank()) {
                project.setName(ProjectEntity.INITIAL_NAME);
            }
            if (project.getStartedAt() == null) project.setStartedAt(now);
            if (project.getCreatedAt() == null) project.setCreatedAt(now);
            project.setUpdatedAt(now);
            requireAffected(projectMapper.insert(project));
        } else {
            project.setUpdatedAt(now);
            requireAffected(projectMapper.updateById(project));
        }
        return project;
    }

    @Override
    public boolean updateName(Long projectId, Long expectedVersion, String name,
                              ProjectNameSource source, ProjectNameSource requiredCurrentSource) {
        LambdaUpdateWrapper<ProjectEntity> update = new LambdaUpdateWrapper<ProjectEntity>()
                .eq(ProjectEntity::getId, projectId)
                .eq(ProjectEntity::getVersion, expectedVersion)
                .set(ProjectEntity::getName, name)
                .set(ProjectEntity::getNameSource, source)
                .set(ProjectEntity::getVersion, expectedVersion + 1)
                .set(ProjectEntity::getUpdatedAt, LocalDateTime.now());
        if (requiredCurrentSource != null) {
            update.eq(ProjectEntity::getNameSource, requiredCurrentSource);
        }
        return projectMapper.update(null, update) == 1;
    }

    private void requireAffected(int affectedRows) {
        if (affectedRows != 1) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "项目已被其他操作更新，请刷新后重试");
        }
    }
}
