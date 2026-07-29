package com.advertisementdesign.back.project.repository;

import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.ProjectStatus;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    List<ProjectEntity> findAll(ProjectStatus status, String keyword);

    Optional<ProjectEntity> findById(Long id);

    ProjectEntity save(ProjectEntity project);
}
