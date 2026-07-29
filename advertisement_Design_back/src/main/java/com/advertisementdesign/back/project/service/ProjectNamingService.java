package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;
import com.advertisementdesign.back.common.audit.service.AuditLogWriter;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.ProjectNameSource;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectNamingService {
    private static final int MAX_NAME_LENGTH = 100;

    private final ProjectRepository repository;
    private final ProjectAuthorizationService authorizationService;
    private final CurrentActorProvider currentActorProvider;
    private final CurrentUserProfileProvider currentUserProfileProvider;
    private final AuditLogWriter auditLogWriter;
    private final ProjectConverter converter;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProjectModels.ProjectFullDetailView renameManually(Long projectId, String requestedName,
                                                               Long expectedVersion) {
        ProjectAuthorizationService.AuthorizationDecision decision = requireCustomerUpdateAuthorization(projectId);
        ProjectEntity before = requireProject(projectId);
        String name = normalizeName(requestedName);
        if (!repository.updateName(projectId, expectedVersion, name, ProjectNameSource.MANUAL, null)) {
            throw conflict();
        }
        ProjectEntity after = requireProject(projectId);
        audit(before, after, "PROJECT_NAME_MANUALLY_UPDATED", decision,
                currentActorProvider.requireCurrentActor().actor(), AuditLogEntity.Source.CUSTOMER_UI);
        return converter.toFullDetail(after);
    }

    @Transactional
    public ProjectModels.ProjectFullDetailView restoreAutomatic(Long projectId, Long expectedVersion) {
        ProjectAuthorizationService.AuthorizationDecision decision = requireCustomerUpdateAuthorization(projectId);
        ProjectEntity before = requireProject(projectId);
        if (!repository.updateName(projectId, expectedVersion, before.getName(), ProjectNameSource.AUTO, null)) {
            throw conflict();
        }
        ProjectEntity after = requireProject(projectId);
        audit(before, after, "PROJECT_AUTOMATIC_NAMING_RESTORED", decision,
                currentActorProvider.requireCurrentActor().actor(), AuditLogEntity.Source.CUSTOMER_UI);
        return converter.toFullDetail(after);
    }

    /**
     * Internal post-commit boundary. Requirement stability is represented by the confirmed immutable
     * requirement version; AUTO and version are both checked atomically so a racing manual rename wins.
     */
    @Transactional
    public boolean applyAutomaticName(Long projectId, Long expectedVersion, String generatedName) {
        ProjectEntity before = requireProject(projectId);
        if (before.getConfirmedRequirementVersionId() == null
                || before.getNameSource() != ProjectNameSource.AUTO) {
            return false;
        }
        String name = normalizeName(generatedName);
        boolean updated = repository.updateName(projectId, expectedVersion, name,
                ProjectNameSource.AUTO, ProjectNameSource.AUTO);
        if (!updated) {
            return false;
        }
        ProjectEntity after = requireProject(projectId);
        ActorRef system = new ActorRef(ActorRef.ActorType.SYSTEM_EVENT, null);
        audit(before, after, "PROJECT_NAME_AUTOMATICALLY_UPDATED", null,
                system, AuditLogEntity.Source.AUTOMATION);
        return true;
    }

    private ProjectAuthorizationService.AuthorizationDecision requireCustomerUpdateAuthorization(Long projectId) {
        ActorRef actor = currentActorProvider.requireCurrentActor().actor();
        if (actor.type() != ActorRef.ActorType.CUSTOMER_USER) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        ProjectAuthorizationService.AuthorizationDecision decision = authorizationService.authorize(
                projectId, ProjectAuthorizationService.ProjectAction.UPDATE_PROJECT);
        if (!decision.allowed()) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return decision;
    }

    private ProjectEntity requireProject(Long projectId) {
        return repository.findById(projectId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    private String normalizeName(String requestedName) {
        if (requestedName == null || requestedName.isBlank()) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "项目名称不能为空");
        }
        String normalized = requestedName.strip().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new ApiException(ApiErrorCode.BAD_REQUEST.getCode(), "项目名称不能超过 100 个字符");
        }
        return normalized;
    }

    private ApiException conflict() {
        return new ApiException(ApiErrorCode.CONFLICT.getCode(), "项目名称已被其他操作更新，请刷新后重试");
    }

    private void audit(ProjectEntity before, ProjectEntity after, String action,
                       ProjectAuthorizationService.AuthorizationDecision decision,
                       ActorRef actor, AuditLogEntity.Source source) {
        Map<String, Object> basis = decision == null || decision.basis() == null
                ? Map.of("source", "POST_COMMIT_AUTOMATION")
                : objectMapper.convertValue(decision.basis(), Map.class);
        String displayIdentity = actor.type() == ActorRef.ActorType.CUSTOMER_USER
                ? currentUserProfileProvider.currentUserProfile().nickname()
                : "项目服务团队";
        auditLogWriter.append(new AuditLogWriter.Entry(
                before.getId(), actor, displayIdentity, source, "PROJECT", before.getId(),
                String.valueOf(after.getVersion()), action, basis,
                Map.of("name", before.getName(), "nameSource", before.getNameSource().name(),
                        "version", before.getVersion()),
                Map.of("name", after.getName(), "nameSource", after.getNameSource().name(),
                        "version", after.getVersion()),
                AuditLogEntity.Result.SUCCESS, null, action + ":" + before.getId() + ":" + after.getVersion(),
                "project-name:" + before.getId(), LocalDateTime.now()));
    }
}
