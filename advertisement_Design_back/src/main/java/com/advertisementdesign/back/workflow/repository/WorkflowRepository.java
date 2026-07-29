package com.advertisementdesign.back.workflow.repository;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.workflow.entity.ProjectStageEventEntity;
import com.advertisementdesign.back.workflow.entity.ProjectStageInstanceEntity;
import com.advertisementdesign.back.workflow.mapper.ProjectStageEventMapper;
import com.advertisementdesign.back.workflow.mapper.ProjectStageInstanceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WorkflowRepository {
    private final ProjectStageInstanceMapper instanceMapper;
    private final ProjectStageEventMapper eventMapper;

    public List<ProjectStageInstanceEntity> findStages(Long projectId) {
        return instanceMapper.selectList(new LambdaQueryWrapper<ProjectStageInstanceEntity>()
                .eq(ProjectStageInstanceEntity::getProjectId, projectId)
                .orderByAsc(ProjectStageInstanceEntity::getSortOrder));
    }

    public Optional<ProjectStageInstanceEntity> findStage(Long projectId, Long stageInstanceId) {
        return Optional.ofNullable(instanceMapper.selectOne(new LambdaQueryWrapper<ProjectStageInstanceEntity>()
                .eq(ProjectStageInstanceEntity::getProjectId, projectId)
                .eq(ProjectStageInstanceEntity::getId, stageInstanceId).last("LIMIT 1")));
    }

    public List<ProjectStageEventEntity> findEvents(Long projectId, Long stageInstanceId) {
        return eventMapper.selectList(new LambdaQueryWrapper<ProjectStageEventEntity>()
                .eq(ProjectStageEventEntity::getProjectId, projectId)
                .eq(ProjectStageEventEntity::getStageInstanceId, stageInstanceId)
                .orderByAsc(ProjectStageEventEntity::getOccurredAt)
                .orderByAsc(ProjectStageEventEntity::getId));
    }

    public void insertInitialStages(List<ProjectStageInstanceEntity> stages) {
        for (ProjectStageInstanceEntity stage : stages) requireInserted(instanceMapper.insert(stage));
    }

    public ProjectStageEventEntity appendEvent(ProjectStageEventEntity event) {
        if (event.getId() != null) throw new IllegalArgumentException("Stage events are append-only");
        requireInserted(eventMapper.insert(event));
        return event;
    }

    private void requireInserted(int affected) {
        if (affected != 1) throw new ApiException(ApiErrorCode.CONFLICT);
    }
}
