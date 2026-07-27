package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.audit.entity.OperationLogEntity;
import com.advertisementdesign.back.common.audit.repository.AuditRepository;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectStageEntity;
import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
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
    private final AuditRepository auditRepository;
    private final CommunicationRepository communicationRepository;
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
    public ProjectModels.ProjectVO create(ProjectModels.CreateProjectRequest request) {
        ensureDesigner();
        if (request.customerId() == null || request.designerId() == null || request.name() == null || request.name().isBlank()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST);
        }
        ProjectEntity project = ProjectEntity.builder()
                .name(request.name())
                .customerId(request.customerId())
                .designerId(request.designerId())
                .description(request.description())
                .currentStage("REQUIREMENT_GUIDE")
                .status(ProjectStatus.IN_PROGRESS)
                .progress(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        project = projectRepository.saveProject(project);
        ensureConversation(project);
        createDefaultStages(project.getId());
        auditRepository.save(OperationLogEntity.builder()
                .operatorId(authService.currentUserProfile().id())
                .operatorRole(MessageSenderRole.DESIGNER)
                .bizType("PROJECT")
                .bizId(project.getId())
                .action("CREATE")
                .description("创建项目")
                .afterData(java.util.Map.of("status", project.getStatus().name()))
                .createdAt(LocalDateTime.now())
                .build());
        return converter.toProjectVO(project);
    }

    @Transactional
    public ProjectModels.ProjectVO update(Long id, ProjectModels.UpdateProjectRequest request) {
        ProjectEntity project = findAllowedProject(id);
        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.designerId() != null) {
            project.setDesignerId(request.designerId());
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
        ProjectEntity project = findAllowedProject(id);
        project.setStatus(ProjectStatus.CANCELLED);
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
        CurrentUser currentUser = com.advertisementdesign.back.common.web.AuthContext.currentUser();
        if (currentUser.getRole() == UserRole.CUSTOMER && !Objects.equals(project.getCustomerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (currentUser.getRole() == UserRole.DESIGNER && !Objects.equals(project.getDesignerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return project;
    }

    private void ensureDesigner() {
        if (authService.currentUserProfile().role() != UserRole.DESIGNER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private void ensureConversation(ProjectEntity project) {
        communicationRepository.findConversationByProjectId(project.getId()).orElseGet(() ->
                communicationRepository.saveConversation(ConversationEntity.builder()
                        .projectId(project.getId())
                        .customerId(project.getCustomerId())
                        .designerId(project.getDesignerId())
                        .lastMessage(null)
                        .lastMessageAt(null)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
        );
    }

    private void createDefaultStages(Long projectId) {
        String[][] stages = {
                {"REQUIREMENT_GUIDE", "需求引导"},
                {"CONTRACT_PREPAYMENT", "签订合同预付款"},
                {"RESEARCH_REPORT", "资料调研报告"},
                {"SKETCH_STYLE", "草图风格敲定"},
                {"REVIEW_FINAL", "审稿定稿"},
                {"FINAL_PAYMENT", "交付尾款"},
                {"AFTER_SALE_REPURCHASE", "售后复购"}
        };
        for (int i = 0; i < stages.length; i++) {
            String[] stage = stages[i];
            final int sortOrder = i + 1;
            if (projectRepository.findStage(projectId, stage[0]).isEmpty()) {
                ProjectStageEntity entity = ProjectStageEntity.builder()
                        .projectId(projectId)
                        .stageCode(stage[0])
                        .stageName(stage[1])
                        .sortOrder(sortOrder)
                        .status(ProjectStageStatus.TODO)
                        .reachedAt(null)
                        .updatedAt(LocalDateTime.now())
                        .build();
                projectRepository.saveStage(entity);
            }
        }
    }
}
