package com.advertisementdesign.back.project.repository.mysql;

import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectStageEntity;
import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.mapper.ProjectFileMapper;
import com.advertisementdesign.back.project.mapper.ProjectMapper;
import com.advertisementdesign.back.project.mapper.ProjectStageMapper;
import com.advertisementdesign.back.project.mapper.StageActionMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MybatisPlusProjectRepositoryTest {
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectStageMapper projectStageMapper;
    @Mock private StageActionMapper stageActionMapper;
    @Mock private ProjectFileMapper projectFileMapper;

    private MybatisPlusProjectRepository repository;

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant projectAssistant = new MapperBuilderAssistant(configuration, "project-test");
        projectAssistant.setCurrentNamespace(ProjectMapper.class.getName());
        TableInfoHelper.initTableInfo(projectAssistant, ProjectEntity.class);
        MapperBuilderAssistant stageAssistant = new MapperBuilderAssistant(configuration, "project-stage-test");
        stageAssistant.setCurrentNamespace(ProjectStageMapper.class.getName());
        TableInfoHelper.initTableInfo(stageAssistant, ProjectStageEntity.class);
    }

    @BeforeEach
    void setUp() {
        repository = new MybatisPlusProjectRepository(projectMapper, projectStageMapper,
                stageActionMapper, projectFileMapper);
    }

    @Test
    void refreshProgressUsesReachedStageCountAndCompletesAtSeven() {
        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .status(ProjectStatus.IN_PROGRESS)
                .progress(0)
                .build();
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(projectStageMapper.selectCount(any())).thenReturn(7L);

        repository.refreshProjectProgress(1L);

        assertEquals(100, project.getProgress());
        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
        verify(projectMapper).updateById(project);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<ProjectStageEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(projectStageMapper).selectCount(captor.capture());
        AbstractWrapper<ProjectStageEntity, ?, ?> wrapper =
                (AbstractWrapper<ProjectStageEntity, ?, ?>) captor.getValue();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(1L));
        assertTrue(wrapper.getParamNameValuePairs()
                .containsValue(ProjectStageStatus.REACHED));
    }

    @Test
    void refreshProgressRoundsAgainstFixedSevenStagesWithoutChangingStatus() {
        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .status(ProjectStatus.IN_PROGRESS)
                .progress(0)
                .build();
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(projectStageMapper.selectCount(any())).thenReturn(2L);

        repository.refreshProjectProgress(1L);

        assertEquals(29, project.getProgress());
        assertEquals(ProjectStatus.IN_PROGRESS, project.getStatus());
        verify(projectMapper).updateById(project);
    }
}
