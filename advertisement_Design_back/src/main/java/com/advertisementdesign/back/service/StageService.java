package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.ApiAssembler;
import com.advertisementdesign.back.api.stage.StageModels;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.AuthContext;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.domain.entity.ConversationEntity;
import com.advertisementdesign.back.domain.entity.OperationLogEntity;
import com.advertisementdesign.back.domain.entity.ProjectEntity;
import com.advertisementdesign.back.domain.entity.ProjectStageEntity;
import com.advertisementdesign.back.domain.entity.StageActionEntity;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.ProjectStageStatus;
import com.advertisementdesign.back.domain.enums.StageActionStatus;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StageService {
    private final DemoDataStore store;
    private final ApiAssembler assembler;
    private final AuthService authService;

    public List<StageModels.StageActionVO> list(Long projectId, String stageCode, StageActionStatus status) {
        ProjectEntity project = findAllowedProject(projectId);
        return store.listStageActions(project.getId(), stageCode, status).stream()
                .map(assembler::toStageActionVO)
                .toList();
    }

    public StageModels.StageActionVO createAction(Long projectId, String stageCode, StageModels.CreateStageActionRequest request) {
        ProjectEntity project = findAllowedProject(projectId);
        ProjectStageEntity stage = store.findStage(project.getId(), stageCode)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserEntity currentUser = authService.currentUserEntity();
        UserEntity confirmUser = Objects.equals(project.getCustomerId(), currentUser.getId())
                ? store.findUserById(project.getDesignerId()).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND))
                : store.findUserById(project.getCustomerId()).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        StageActionEntity action = StageActionEntity.builder()
                .projectId(project.getId())
                .projectStageId(stage.getId())
                .stageCode(stageCode)
                .initiatorId(currentUser.getId())
                .initiatorRole(MessageSenderRole.valueOf(currentUser.getRole().name()))
                .confirmUserId(confirmUser.getId())
                .status(StageActionStatus.PENDING)
                .requestNote(request == null ? null : request.requestNote())
                .responseNote(null)
                .requestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        action = store.saveStageAction(action);
        stage.setStatus(ProjectStageStatus.PENDING_CONFIRM);
        store.saveStage(stage);
        project.setCurrentStage(stageCode);
        store.saveProject(project);
        addStageSystemMessage(project, "阶段「" + stage.getStageName() + "」等待对方确认。");
        saveLog(currentUser, "REQUEST_CONFIRM", "发起阶段确认", project.getId(), action.getId(), stage.getStatus().name(), ProjectStageStatus.PENDING_CONFIRM.name());
        return assembler.toStageActionVO(action);
    }

    public StageModels.StageActionVO confirm(Long actionId, StageModels.StageActionResponseRequest request) {
        StageActionEntity action = store.findStageActionById(actionId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserEntity currentUser = authService.currentUserEntity();
        if (!Objects.equals(action.getConfirmUserId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (action.getStatus() != StageActionStatus.PENDING) {
            throw new ApiException(400, "阶段动作状态不允许确认");
        }
        action.setStatus(StageActionStatus.CONFIRMED);
        action.setResponseNote(request == null ? null : request.responseNote());
        action.setRespondedAt(LocalDateTime.now());
        store.saveStageAction(action);
        updateStageAndProject(action.getProjectStageId(), action.getProjectId(), ProjectStageStatus.REACHED);
        addStageSystemMessage(findAllowedProject(action.getProjectId()), "阶段「" + action.getStageCode() + "」已达成。");
        saveLog(currentUser, "CONFIRM", "确认阶段", action.getProjectId(), action.getId(), StageActionStatus.PENDING.name(), StageActionStatus.CONFIRMED.name());
        return assembler.toStageActionVO(action);
    }

    public StageModels.StageActionVO reject(Long actionId, StageModels.StageActionResponseRequest request) {
        StageActionEntity action = store.findStageActionById(actionId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        UserEntity currentUser = authService.currentUserEntity();
        if (!Objects.equals(action.getConfirmUserId(), currentUser.getId())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (action.getStatus() != StageActionStatus.PENDING) {
            throw new ApiException(400, "阶段动作状态不允许驳回");
        }
        action.setStatus(StageActionStatus.REJECTED);
        action.setResponseNote(request == null ? null : request.responseNote());
        action.setRespondedAt(LocalDateTime.now());
        store.saveStageAction(action);
        updateStageAndProject(action.getProjectStageId(), action.getProjectId(), ProjectStageStatus.REJECTED);
        addStageSystemMessage(findAllowedProject(action.getProjectId()), "阶段「" + action.getStageCode() + "」已驳回。");
        saveLog(currentUser, "REJECT", "驳回阶段", action.getProjectId(), action.getId(), StageActionStatus.PENDING.name(), StageActionStatus.REJECTED.name());
        return assembler.toStageActionVO(action);
    }

    private void updateStageAndProject(Long projectStageId, Long projectId, ProjectStageStatus status) {
        ProjectStageEntity stage = store.findStageById(projectStageId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        stage.setStatus(status);
        if (status == ProjectStageStatus.REACHED) {
            stage.setReachedAt(LocalDateTime.now());
        }
        store.saveStage(stage);
        store.refreshProjectProgress(projectId);
    }

    private void addStageSystemMessage(ProjectEntity project, String content) {
        ConversationEntity conversation = store.findConversationByProjectId(project.getId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        var message = com.advertisementdesign.back.domain.entity.MessageEntity.builder()
                .conversationId(conversation.getId())
                .senderId(null)
                .senderRole(MessageSenderRole.SYSTEM)
                .messageType(com.advertisementdesign.back.domain.enums.MessageType.SYSTEM)
                .content(content)
                .replyToMessageId(null)
                .clientMessageId(null)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .fileIds(List.of())
                .build();
        store.saveMessage(message);
        conversation.setLastMessage(content);
        conversation.setLastMessageAt(LocalDateTime.now());
        store.saveConversation(conversation);
    }

    private void saveLog(UserEntity user, String action, String description, Long projectId, Long bizId, String before, String after) {
        OperationLogEntity log = OperationLogEntity.builder()
                .operatorId(user.getId())
                .operatorRole(MessageSenderRole.valueOf(user.getRole().name()))
                .bizType("STAGE")
                .bizId(projectId)
                .action(action)
                .description(description)
                .beforeData(java.util.Map.of("status", before))
                .afterData(java.util.Map.of("status", after))
                .createdAt(LocalDateTime.now())
                .build();
        store.saveOperationLog(log);
    }

    private ProjectEntity findAllowedProject(Long id) {
        ProjectEntity project = store.findProjectById(id).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
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
