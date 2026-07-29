package com.advertisementdesign.back.project.repository;

import com.advertisementdesign.back.project.entity.ProjectAssignmentEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectAssignmentRepository {
    Optional<ProjectAssignmentEntity> findEffectiveAssignment(
            Long projectId, Long designerUserId, LocalDateTime effectiveAt);

    Optional<ProjectAssignmentEntity> findCurrentResponsibleDesigner(Long projectId, LocalDateTime effectiveAt);

    List<ProjectAssignmentEntity> findHistory(Long projectId);
}
