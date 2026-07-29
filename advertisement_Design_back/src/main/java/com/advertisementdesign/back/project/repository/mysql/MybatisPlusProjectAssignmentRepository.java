package com.advertisementdesign.back.project.repository.mysql;

import com.advertisementdesign.back.project.entity.ProjectAssignmentEntity;
import com.advertisementdesign.back.project.enums.ProjectAssignmentRole;
import com.advertisementdesign.back.project.enums.ProjectAssignmentStatus;
import com.advertisementdesign.back.project.mapper.ProjectAssignmentMapper;
import com.advertisementdesign.back.project.repository.ProjectAssignmentRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisPlusProjectAssignmentRepository implements ProjectAssignmentRepository {
    private final ProjectAssignmentMapper mapper;

    @Override
    public Optional<ProjectAssignmentEntity> findEffectiveAssignment(
            Long projectId, Long designerUserId, LocalDateTime effectiveAt) {
        return Optional.ofNullable(mapper.selectOne(effectiveQuery(projectId, effectiveAt)
                .eq(ProjectAssignmentEntity::getDesignerUserId, designerUserId)
                .last("LIMIT 1")));
    }

    @Override
    public Optional<ProjectAssignmentEntity> findCurrentResponsibleDesigner(Long projectId, LocalDateTime effectiveAt) {
        return Optional.ofNullable(mapper.selectOne(effectiveQuery(projectId, effectiveAt)
                .eq(ProjectAssignmentEntity::getAssignmentRole, ProjectAssignmentRole.PRIMARY_DESIGNER)
                .last("LIMIT 1")));
    }

    @Override
    public List<ProjectAssignmentEntity> findHistory(Long projectId) {
        return mapper.selectList(new LambdaQueryWrapper<ProjectAssignmentEntity>()
                .eq(ProjectAssignmentEntity::getProjectId, projectId)
                .orderByDesc(ProjectAssignmentEntity::getCreatedAt)
                .orderByDesc(ProjectAssignmentEntity::getId));
    }

    private LambdaQueryWrapper<ProjectAssignmentEntity> effectiveQuery(Long projectId, LocalDateTime effectiveAt) {
        return new LambdaQueryWrapper<ProjectAssignmentEntity>()
                .eq(ProjectAssignmentEntity::getProjectId, projectId)
                .eq(ProjectAssignmentEntity::getStatus, ProjectAssignmentStatus.ACTIVE)
                .le(ProjectAssignmentEntity::getEffectiveFrom, effectiveAt)
                .and(query -> query.isNull(ProjectAssignmentEntity::getEffectiveTo)
                        .or().ge(ProjectAssignmentEntity::getEffectiveTo, effectiveAt));
    }
}
