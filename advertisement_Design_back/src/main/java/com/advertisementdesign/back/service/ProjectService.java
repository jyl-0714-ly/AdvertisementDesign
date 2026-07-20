package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.project.ProjectModels;
import com.advertisementdesign.back.common.api.PageResult;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.domain.entity.ConversationEntity;
import com.advertisementdesign.back.domain.entity.ProjectEntity;
import com.advertisementdesign.back.domain.entity.ProjectStageEntity;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.ProjectStageStatus;
import com.advertisementdesign.back.domain.enums.ProjectStatus;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final DemoDataStore store;
    private final ApiAssembler assembler;
    private final AuthService authService;

    public PageResult<ProjectModels.ProjectVO> list(String status, String currentStage, String keyword, long page, long size) {
        UserEntity currentUser = authService.currentUserEntity();
        List<ProjectEntity> filtered = store.listProjects().stream()
                .filter(project -> currentUser.getRole() == UserRole.DESIGNER ? Objects.equals(project.getDesignerId(), currentUser.getId()) : Objects.equals(project.getCustomerId(), currentUser.getId()))
                .filter(project -> status == null || status.isBlank() || project.getStatus().name().equalsIgnoreCase(status))
                .filter(project -> currentStage == null || currentStage.isBlank() || project.getCurrentStage().equalsIgnoreCase(currentStage))
                .filter(project -> keyword == null || keyword.isBlank() || project.getName().contains(keyword) || (project.getDescription() != null && project.getDescription().contains(keyword)))
                .toList();
        List<ProjectModels.ProjectVO> records = filtered.stream()
                .skip(Math.max(page - 1, 0) * size)
                .limit(size)
                .map(assembler::toProjectVO)
                .toList();
        return PageResult.of(records, filtered.size(), page, size);
    }

    public ProjectModels.ProjectVO detail(Long id) {
        ProjectEntity project = findAllowedProject(id);
        return assembler.toProjectVO(project);
    }

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
        project = store.saveProject(project);
        ensureConversation(project);
        createDefaultStages(project.getId());
        store.saveOperationLog(com.advertisementdesign.back.domain.entity.OperationLogEntity.builder()
                .operatorId(authService.currentUserEntity().getId())
                .operatorRole(MessageSenderRole.DESIGNER)
                .bizType("PROJECT")
                .bizId(project.getId())
                .action("CREATE")
                .description("创建项目")
                .afterData(java.util.Map.of("status", project.getStatus().name()))
                .createdAt(LocalDateTime.now())
                .build());
        return assembler.toProjectVO(project);
    }

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
        store.saveProject(project);
        return assembler.toProjectVO(project);
    }

    public boolean delete(Long id) {
        ProjectEntity project = findAllowedProject(id);
        project.setStatus(ProjectStatus.CANCELLED);
        project.setUpdatedAt(LocalDateTime.now());
        store.saveProject(project);
        return true;
    }

    public List<ProjectModels.ProjectStageVO> stages(Long projectId) {
        ProjectEntity project = findAllowedProject(projectId);
        return store.listStages(project.getId()).stream().map(assembler::toProjectStageVO).toList();
    }

    private ProjectEntity findAllowedProject(Long id) {
        ProjectEntity project = store.findProjectById(id).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
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
        if (authService.currentUserEntity().getRole() != UserRole.DESIGNER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private void ensureConversation(ProjectEntity project) {
        store.findConversationByProjectId(project.getId()).orElseGet(() ->
                store.saveConversation(ConversationEntity.builder()
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
            if (store.findStage(projectId, stage[0]).isEmpty()) {
                ProjectStageEntity entity = ProjectStageEntity.builder()
                        .projectId(projectId)
                        .stageCode(stage[0])
                        .stageName(stage[1])
                        .sortOrder(sortOrder)
                        .status(ProjectStageStatus.TODO)
                        .reachedAt(null)
                        .updatedAt(LocalDateTime.now())
                        .build();
                store.saveStage(entity);
            }
        }
    }
}
