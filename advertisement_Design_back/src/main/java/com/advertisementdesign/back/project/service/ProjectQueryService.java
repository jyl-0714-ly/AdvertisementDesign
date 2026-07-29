package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.enums.CustomerProjectMemberStatus;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.repository.CustomerProjectMemberRepository;
import com.advertisementdesign.back.project.repository.ProjectAssignmentRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectQueryService {
    private final ProjectRepository projectRepository;
    private final CustomerProjectMemberRepository customerMemberRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectConverter converter;

    public Optional<ProjectModels.ProjectContextView> findContext(Long projectId) {
        return projectRepository.findById(projectId).map(converter::toContext);
    }

    public List<ProjectModels.ProjectSummaryView> listAuthorizedSummaries(String status, String keyword) {
        com.advertisementdesign.back.project.enums.ProjectStatus parsedStatus = parseStatus(status);
        return projectRepository.findAll(parsedStatus, keyword).stream()
                .filter(project -> authorizationService.authorize(
                        project.getId(), ProjectAuthorizationService.ProjectAction.VIEW_SUMMARY).allowed())
                .map(converter::toSummary)
                .toList();
    }

    public ProjectModels.ProjectSummaryView requireSummary(Long projectId) {
        requireAuthorized(projectId, ProjectAuthorizationService.ProjectAction.VIEW_SUMMARY);
        return converter.toSummary(requireProject(projectId));
    }

    public ProjectModels.ProjectFullDetailView requireFullDetail(Long projectId) {
        requireAuthorized(projectId, ProjectAuthorizationService.ProjectAction.VIEW_FULL);
        return converter.toFullDetail(requireProject(projectId));
    }

    public List<ProjectModels.CustomerMemberView> listActiveCustomerMembers(Long projectId) {
        requireAuthorized(projectId, ProjectAuthorizationService.ProjectAction.VIEW_FULL);
        return customerMemberRepository.findByProject(projectId, CustomerProjectMemberStatus.ACTIVE)
                .stream().map(converter::toCustomerMember).toList();
    }

    public Optional<ProjectModels.AssignmentView> currentResponsibleDesigner(Long projectId) {
        requireAuthorized(projectId, ProjectAuthorizationService.ProjectAction.ADJUST_ASSIGNMENT);
        return assignmentRepository.findCurrentResponsibleDesigner(projectId, LocalDateTime.now())
                .map(converter::toAssignment);
    }

    public List<ProjectModels.AssignmentView> assignmentHistory(Long projectId) {
        requireAuthorized(projectId, ProjectAuthorizationService.ProjectAction.ADJUST_ASSIGNMENT);
        return assignmentRepository.findHistory(projectId).stream().map(converter::toAssignment).toList();
    }

    private com.advertisementdesign.back.project.entity.ProjectEntity requireProject(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    private void requireAuthorized(Long projectId, ProjectAuthorizationService.ProjectAction action) {
        if (!authorizationService.authorize(projectId, action).allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private com.advertisementdesign.back.project.enums.ProjectStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return com.advertisementdesign.back.project.enums.ProjectStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "无效的项目状态");
        }
    }
}
