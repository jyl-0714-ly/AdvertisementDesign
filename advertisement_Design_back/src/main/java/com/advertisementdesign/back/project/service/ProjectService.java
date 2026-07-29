package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectConverter converter;
    private final AuthService authService;

    public PageResult<ProjectModels.ProjectVO> list(String status, String currentStage, String keyword, long page, long size) {
        UserProfile currentUser = authService.currentUserProfile();
        List<ProjectEntity> filtered = projectRepository.listProjects().stream()
                .filter(project -> currentUser.role() == UserRole.DESIGNER ? Objects.equals(project.getDesignerId(), currentUser.id()) : Objects.equals(project.getCustomerId(), currentUser.id()))
                .filter(project -> status == null || status.isBlank() || project.getStatus().name().equalsIgnoreCase(status))
                .filter(project -> currentStage == null || currentStage.isBlank() || project.getCurrentStage().equalsIgnoreCase(currentStage))
                .filter(project -> keyword == null || keyword.isBlank() || project.getName().contains(keyword) || (project.getDescription() != null && project.getDescription().contains(keyword)))
                .toList();
        List<ProjectModels.ProjectVO> records = filtered.stream()
                .skip(Math.max(page - 1, 0) * size)
                .limit(size)
                .map(converter::toProjectVO)
                .toList();
        return PageResult.of(records, filtered.size(), page, size);
    }

    public ProjectModels.ProjectVO detail(Long id) {
        ProjectEntity project = findAllowedProject(id);
        return converter.toProjectVO(project);
    }

    @Transactional
    public ProjectModels.ProjectVO update(Long id, ProjectModels.UpdateProjectRequest request) {
        ensureDesigner();
        ProjectEntity project = findAllowedProject(id);
        if (request.designerId() != null) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "不允许通过项目更新接口变更设计师");
        }
        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.saveProject(project);
        return converter.toProjectVO(project);
    }

    @Transactional
    public boolean delete(Long id) {
        ensureDesigner();
        ProjectEntity project = findAllowedProject(id);
        project.setStatus(ProjectStatus.CANCELLED);
        project.setCancelledAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.saveProject(project);
        return true;
    }

    public List<ProjectModels.ProjectStageVO> stages(Long projectId) {
        ProjectEntity project = findAllowedProject(projectId);
        return projectRepository.listStages(project.getId()).stream().map(converter::toProjectStageVO).toList();
    }

    private ProjectEntity findAllowedProject(Long id) {
        ProjectEntity project = projectRepository.findProjectById(id).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserProfile currentUser = authService.currentUserProfile();
        if (currentUser.role() == UserRole.CUSTOMER && !Objects.equals(project.getCustomerId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (currentUser.role() == UserRole.DESIGNER && !Objects.equals(project.getDesignerId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return project;
    }

    private void ensureDesigner() {
        if (authService.currentUserProfile().role() != UserRole.DESIGNER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
