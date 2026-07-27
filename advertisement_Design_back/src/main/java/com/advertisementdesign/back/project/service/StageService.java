package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.audit.entity.OperationLogEntity;
import com.advertisementdesign.back.common.audit.repository.AuditRepository;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.AuthContext;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.entity.ProjectStageEntity;
import com.advertisementdesign.back.project.entity.StageActionEntity;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import com.advertisementdesign.back.project.enums.StageActionStatus;
import com.advertisementdesign.back.project.model.StageModels;
import com.advertisementdesign.back.communication.repository.CommunicationRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StageService {
    private final ProjectRepository projectRepository;
    private final AuditRepository auditRepository;
    private final CommunicationRepository communicationRepository;
    private final ProjectConverter converter;
    private final AuthService authService;
    private final IdentityService identityService;

    public List<StageModels.StageActionVO> list(Long projectId, String stageCode, StageActionStatus status) {
        ProjectEntity project = findAllowedProject(projectId);
        return projectRepository.listStageActions(project.getId(), stageCode, status).stream()
                .map(converter::toStageActionVO)
                .toList();
    }

    @Transactional
    public StageModels.StageActionVO createAction(Long projectId, String stageCode, StageModels.CreateStageActionRequest request) {
        ProjectEntity project = findAllowedProject(projectId);
        ProjectStageEntity stage = projectRepository.findStage(project.getId(), stageCode)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserProfile currentUser = authService.currentUserProfile();
        Long confirmUserId = Objects.equals(project.getCustomerId(), currentUser.id())
                ? project.getDesignerId()
                : project.getCustomerId();
        if (identityService.findById(confirmUserId).isEmpty()) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        StageActionEntity action = StageActionEntity.builder()
                .projectId(project.getId())
                .projectStageId(stage.getId())
                .stageCode(stageCode)
                .initiatorId(currentUser.id())
                .initiatorRole(MessageSenderRole.valueOf(currentUser.role().name()))
                .confirmUserId(confirmUserId)
                .status(StageActionStatus.PENDING)
                .requestNote(request == null ? null : request.requestNote())
                .responseNote(null)
                .requestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        action = projectRepository.saveStageAction(action);
        String previousStageStatus = stage.getStatus().name();
        stage.setStatus(ProjectStageStatus.PENDING_CONFIRM);
        projectRepository.saveStage(stage);
        project.setCurrentStage(stageCode);
        projectRepository.saveProject(project);
        addStageSystemMessage(project, "阶段「" + stage.getStageName() + "」等待对方确认。");
        saveLog(currentUser, "REQUEST_CONFIRM", "发起阶段确认", project.getId(), previousStageStatus, ProjectStageStatus.PENDING_CONFIRM.name());
        return converter.toStageActionVO(action);
    }

    @Transactional
    public StageModels.StageActionVO confirm(Long actionId, StageModels.StageActionResponseRequest request) {
        StageActionEntity action = projectRepository.findStageActionById(actionId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserProfile currentUser = authService.currentUserProfile();
        if (!Objects.equals(action.getConfirmUserId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (action.getStatus() != StageActionStatus.PENDING) {
            throw new ApiException(400, "阶段动作状态不允许确认");
        }
        action.setStatus(StageActionStatus.CONFIRMED);
        action.setResponseNote(request == null ? null : request.responseNote());
        action.setRespondedAt(LocalDateTime.now());
        projectRepository.saveStageAction(action);
        updateStageAndProject(action.getProjectStageId(), action.getProjectId(), ProjectStageStatus.REACHED);
        addStageSystemMessage(findAllowedProject(action.getProjectId()), "阶段「" + action.getStageCode() + "」已达成。");
        saveLog(currentUser, "CONFIRM", "确认阶段", action.getProjectId(), StageActionStatus.PENDING.name(), StageActionStatus.CONFIRMED.name());
        return converter.toStageActionVO(action);
    }

    @Transactional
    public StageModels.StageActionVO reject(Long actionId, StageModels.StageActionResponseRequest request) {
        StageActionEntity action = projectRepository.findStageActionById(actionId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserProfile currentUser = authService.currentUserProfile();
        if (!Objects.equals(action.getConfirmUserId(), currentUser.id())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (action.getStatus() != StageActionStatus.PENDING) {
            throw new ApiException(400, "阶段动作状态不允许驳回");
        }
        action.setStatus(StageActionStatus.REJECTED);
        action.setResponseNote(request == null ? null : request.responseNote());
        action.setRespondedAt(LocalDateTime.now());
        projectRepository.saveStageAction(action);
        updateStageAndProject(action.getProjectStageId(), action.getProjectId(), ProjectStageStatus.REJECTED);
        addStageSystemMessage(findAllowedProject(action.getProjectId()), "阶段「" + action.getStageCode() + "」已驳回。");
        saveLog(currentUser, "REJECT", "驳回阶段", action.getProjectId(), StageActionStatus.PENDING.name(), StageActionStatus.REJECTED.name());
        return converter.toStageActionVO(action);
    }

    private void updateStageAndProject(Long projectStageId, Long projectId, ProjectStageStatus status) {
        ProjectStageEntity stage = projectRepository.findStageById(projectStageId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        stage.setStatus(status);
        if (status == ProjectStageStatus.REACHED) {
            stage.setReachedAt(LocalDateTime.now());
        }
        projectRepository.saveStage(stage);
        projectRepository.refreshProjectProgress(projectId);
    }

    private void addStageSystemMessage(ProjectEntity project, String content) {
        ConversationEntity conversation = communicationRepository.findConversationByProjectId(project.getId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        var message = com.advertisementdesign.back.communication.entity.MessageEntity.builder()
                .conversationId(conversation.getId())
                .senderId(null)
                .senderRole(MessageSenderRole.SYSTEM)
                .messageType(com.advertisementdesign.back.communication.enums.MessageType.SYSTEM)
                .content(content)
                .replyToMessageId(null)
                .clientMessageId(null)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .fileIds(List.of())
                .build();
        communicationRepository.saveMessage(message);
        conversation.setLastMessage(content);
        conversation.setLastMessageAt(LocalDateTime.now());
        communicationRepository.saveConversation(conversation);
    }

    private void saveLog(UserProfile user, String action, String description, Long projectId, String before, String after) {
        OperationLogEntity log = OperationLogEntity.builder()
                .operatorId(user.id())
                .operatorRole(MessageSenderRole.valueOf(user.role().name()))
                .bizType("STAGE")
                .bizId(projectId)
                .action(action)
                .description(description)
                .beforeData(java.util.Map.of("status", before))
                .afterData(java.util.Map.of("status", after))
                .createdAt(LocalDateTime.now())
                .build();
        auditRepository.save(log);
    }

    private ProjectEntity findAllowedProject(Long id) {
        ProjectEntity project = projectRepository.findProjectById(id).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        CurrentUser currentUser = AuthContext.currentUser();
        if (currentUser.getRole() == UserRole.CUSTOMER && !Objects.equals(project.getCustomerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (currentUser.getRole() == UserRole.DESIGNER && !Objects.equals(project.getDesignerId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return project;
    }
}
